package com.chestprotector.backport.client;

import com.chestprotector.backport.ChestProtectorBackport;
import com.chestprotector.backport.PasswordProtected;
import com.chestprotector.backport.network.PinPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class PinScreen extends Screen {
    private static final int PANEL_WIDTH = 184;
    private static final int HEADER_HEIGHT = 40;

    private static final int INPUT_TOP = 50;
    private static final int INPUT_WIDTH = 124;
    private static final int INPUT_HEIGHT = 20;

    private static final int KEY_WIDTH = 34;
    private static final int KEY_HEIGHT = 26;
    private static final int KEY_GAP_X = 8;
    private static final int KEY_GAP_Y = 6;
    private static final int KEY_COLUMNS = 3;
    private static final int KEY_ROWS = 4;
    private static final int GRID_WIDTH = KEY_WIDTH * KEY_COLUMNS + KEY_GAP_X * (KEY_COLUMNS - 1);
    private static final int KEYPAD_TOP = 80;

    // Derived so the panel can never be too short for its contents. The previous layout hard-coded
    // a height that left the confirm bar overlapping the bottom keypad row by 13 pixels.
    private static final int KEYPAD_BOTTOM = KEYPAD_TOP + KEY_ROWS * KEY_HEIGHT + (KEY_ROWS - 1) * KEY_GAP_Y;
    private static final int CONFIRM_TOP = KEYPAD_BOTTOM + 10;
    private static final int PANEL_HEIGHT = CONFIRM_TOP + KEY_HEIGHT + 10;

    private final BlockPos pos;
    private final boolean setup;
    private String pin = "";

    /** A clickable button. Rendering and hit testing both read this, so they cannot disagree. */
    private record Key(int x, int y, int width, int height, Text label, boolean accent, Runnable action) {}

    public PinScreen(BlockPos pos, boolean setup) {
        super(Text.translatable(setup ? "chest_protector.screen.setup.title" : "chest_protector.screen.unlock.title"));
        this.pos = pos;
        this.setup = setup;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            append((char) ('0' + keyCode - GLFW.GLFW_KEY_0));
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            append((char) ('0' + keyCode - GLFW.GLFW_KEY_KP_0));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            backspace();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        for (Key key : keys()) {
            if (inside(mouseX, mouseY, key.x(), key.y(), key.width(), key.height())) {
                key.action().run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The keypad, then the confirm bar. Laid out once and reused by render and click handling. */
    private List<Key> keys() {
        List<Key> keys = new ArrayList<>(KEY_ROWS * KEY_COLUMNS + 1);
        for (int digit = 1; digit <= 9; digit++) {
            char typed = (char) ('0' + digit);
            keys.add(gridKey((digit - 1) % KEY_COLUMNS, (digit - 1) / KEY_COLUMNS,
                Text.literal(String.valueOf(digit)), false, () -> append(typed)));
        }
        // Digits and the backspace arrow stay literal: they are symbols, not prose.
        keys.add(gridKey(0, 3, Text.literal("<-"), true, this::backspace));
        keys.add(gridKey(1, 3, Text.literal("0"), false, () -> append('0')));
        keys.add(gridKey(2, 3, Text.translatable("chest_protector.screen.keypad.confirm"), true, this::submit));
        keys.add(new Key(gridLeft(), top() + CONFIRM_TOP, GRID_WIDTH, KEY_HEIGHT,
            Text.translatable(setup ? "chest_protector.screen.setup.confirm" : "chest_protector.screen.unlock.confirm"),
            true, this::submit));
        return keys;
    }

    private Key gridKey(int column, int row, Text label, boolean accent, Runnable action) {
        return new Key(
            gridLeft() + column * (KEY_WIDTH + KEY_GAP_X),
            top() + KEYPAD_TOP + row * (KEY_HEIGHT + KEY_GAP_Y),
            KEY_WIDTH, KEY_HEIGHT, label, accent, action);
    }

    private void append(char c) {
        if (pin.length() < 6) {
            pin += c;
        }
    }

    private void backspace() {
        if (!pin.isEmpty()) {
            pin = pin.substring(0, pin.length() - 1);
        }
    }

    private void submit() {
        if (pin.isEmpty()) {
            return;
        }
        if (setup) {
            ClientPlayNetworking.send(new PinPayloads.SubmitSetup(pos, pin));
        } else {
            ClientPlayNetworking.send(new PinPayloads.SubmitUnlock(pos, pin));
        }
        close();
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private int gridLeft() {
        return left() + (PANEL_WIDTH - GRID_WIDTH) / 2;
    }

    private boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    /**
     * {@code Screen.render} already calls {@code renderBackground}, which applies the blur and the
     * darkening pass. Calling it here as well and then delegating to {@code super} at the end ran
     * that pass twice, the second time on top of the finished panel, which blurred and dimmed the
     * GUI itself. Everything below is therefore drawn strictly after the single {@code super} call.
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int left = left();
        int top = top();
        int centerX = left + PANEL_WIDTH / 2;

        context.fill(0, 0, width, height, 0x86070B12);
        context.fill(left - 2, top - 2, left + PANEL_WIDTH + 2, top + PANEL_HEIGHT + 2, 0xAA000000);
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF0171A22);
        context.drawBorder(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFFAA8852);

        context.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + HEADER_HEIGHT, 0xEE212734);
        context.fill(left + 1, top + HEADER_HEIGHT, left + PANEL_WIDTH - 1, top + HEADER_HEIGHT + 1, 0xFFD2A86C);

        context.drawCenteredTextWithShadow(textRenderer, title, centerX, top + 11, 0xFFF3E2BC);
        context.drawCenteredTextWithShadow(textRenderer, subtitle(), centerX, top + 25, 0xFF9FA9BC);

        drawInputField(context, left + (PANEL_WIDTH - INPUT_WIDTH) / 2, top + INPUT_TOP);

        for (Key key : keys()) {
            drawButton(context, mouseX, mouseY, key);
        }
    }

    /**
     * Names the owner on the unlock screen. Readable client-side only because the locked state and
     * owner are synced with the block entity; the PIN itself is never sent.
     */
    private Text subtitle() {
        if (setup) return Text.translatable("chest_protector.screen.setup.subtitle");
        String owner = "";
        if (client != null && client.world != null) {
            PasswordProtected data = ChestProtectorBackport.protectionAt(client.world, pos);
            if (data != null) owner = data.chestProtector$getOwnerName();
        }
        return owner.isEmpty()
            ? Text.translatable("chest_protector.screen.unlock.subtitle")
            : Text.translatable("chest_protector.screen.unlock.owner", owner);
    }

    private String maskedPin() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pin.length(); i++) {
            builder.append('\u2022');
        }
        if (pin.length() < 6) {
            builder.append('_');
        }
        return builder.toString();
    }

    private void drawInputField(DrawContext context, int x, int y) {
        context.fill(x, y, x + INPUT_WIDTH, y + INPUT_HEIGHT, 0xFF0E1218);
        context.drawBorder(x, y, INPUT_WIDTH, INPUT_HEIGHT, 0xFF8A6A3A);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(maskedPin()),
            x + INPUT_WIDTH / 2, y + (INPUT_HEIGHT - textRenderer.fontHeight) / 2, 0xFFF8E8C7);
    }

    private void drawButton(DrawContext context, int mouseX, int mouseY, Key key) {
        boolean hovered = inside(mouseX, mouseY, key.x(), key.y(), key.width(), key.height());
        int fill = key.accent() ? (hovered ? 0xFFAA8350 : 0xFF8A663C) : (hovered ? 0xFF394150 : 0xFF2A313E);
        int border = key.accent() ? 0xFFF0CB92 : 0xFF6F7A92;
        int textColor = key.accent() ? 0xFFFDF2DF : 0xFFE1E8F5;

        context.fill(key.x(), key.y(), key.x() + key.width(), key.y() + key.height(), fill);
        context.drawBorder(key.x(), key.y(), key.width(), key.height(), border);
        context.drawCenteredTextWithShadow(textRenderer, key.label(),
            key.x() + key.width() / 2,
            key.y() + (key.height() - textRenderer.fontHeight) / 2,
            textColor);
    }
}
