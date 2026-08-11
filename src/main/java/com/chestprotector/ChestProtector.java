package com.chestprotector;

import com.chestprotector.network.PinPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ChestProtector implements ModInitializer {
    public static final String MOD_ID = "chest_protector";
    public static final Item PASSWORD_PROTECTOR = Registry.register(Registries.ITEM,
        Identifier.of(MOD_ID, "password_protector"), new Item(new Item.Settings().maxCount(16)));

    /**
     * Vanilla's {@code ItemGroups} constants are private, so the group is referenced by its
     * registry id. Without this the item is missing from the creative inventory, and recipe
     * viewers such as JEI build their item list from the creative tabs, so it would not show up
     * there either.
     */
    private static final RegistryKey<ItemGroup> TOOLS_GROUP =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.ofVanilla("tools_and_utilities"));

    @Override public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(TOOLS_GROUP).register(entries -> entries.add(PASSWORD_PROTECTOR));

        PayloadTypeRegistry.playS2C().register(PinPayloads.OpenSetup.ID, PinPayloads.OpenSetup.CODEC);
        PayloadTypeRegistry.playS2C().register(PinPayloads.OpenUnlock.ID, PinPayloads.OpenUnlock.CODEC);
        PayloadTypeRegistry.playC2S().register(PinPayloads.SubmitSetup.ID, PinPayloads.SubmitSetup.CODEC);
        PayloadTypeRegistry.playC2S().register(PinPayloads.SubmitUnlock.ID, PinPayloads.SubmitUnlock.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PinPayloads.SubmitSetup.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!validPin(payload.pin()) || !player.getMainHandStack().isOf(PASSWORD_PROTECTOR) || !near(player, payload.pos())) return;
            if (protect(player, payload.pos(), payload.pin())) {
                player.getMainHandStack().decrement(1);
                player.sendMessage(Text.translatable("chest_protector.message.protected"), true);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(PinPayloads.SubmitUnlock.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!near(player, payload.pos())) return;
            PasswordProtected data = protectionAt(player.getWorld(), payload.pos());
            if (data == null || !data.chestProtector$isProtected() || !data.chestProtector$getPassword().equals(payload.pin())) {
                player.sendMessage(Text.translatable("chest_protector.message.wrong_pin"), true); return;
            }
            open(player, payload.pos());
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof ChestBlockEntity)) return ActionResult.PASS;
            if (world.isClient()) return isProtected(world, pos) || player.getMainHandStack().isOf(PASSWORD_PROTECTOR) ? ActionResult.SUCCESS : ActionResult.PASS;
            ServerPlayerEntity sp = (ServerPlayerEntity) player;
            PasswordProtected data = protectionAt(world, pos);
            if (data != null && data.chestProtector$isProtected()) {
                ServerPlayNetworking.send(sp, new PinPayloads.OpenUnlock(canonical(world, pos)));
                return ActionResult.SUCCESS;
            }
            if (player.getMainHandStack().isOf(PASSWORD_PROTECTOR)) {
                ServerPlayNetworking.send(sp, new PinPayloads.OpenSetup(canonical(world, pos)));
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            PasswordProtected data = protectionAt(world, pos);
            if (data == null || !data.chestProtector$isProtected()) return true;
            if (!data.chestProtector$isOwner(player)) { player.sendMessage(Text.translatable("chest_protector.message.not_owner"), true); return false; }
            clear(world, pos); return true;
        });
    }

    public static boolean validPin(String pin) { return pin != null && pin.matches("\\d{1,6}"); }
    public static boolean near(ServerPlayerEntity p, BlockPos pos) { return p.squaredDistanceTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5) <= 64.0; }
    public static boolean isProtected(World world, BlockPos pos) { PasswordProtected d=protectionAt(world,pos); return d!=null && d.chestProtector$isProtected(); }

    /**
     * Returns the protection data covering {@code pos}. For a double chest both halves are
     * inspected and a protected half always wins, so protecting either half locks the whole
     * inventory. Returns an unprotected half (used by the setup flow) when neither is locked,
     * or {@code null} when {@code pos} is not a chest.
     */
    public static PasswordProtected protectionAt(World world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof PasswordProtected)) return null;
        PasswordProtected unprotected = null;
        for (BlockPos p : linked(world, canonical(world, pos))) {
            BlockEntity be = world.getBlockEntity(p);
            if (be instanceof PasswordProtected d) {
                if (d.chestProtector$isProtected()) return d;
                if (unprotected == null) unprotected = d;
            }
        }
        return unprotected;
    }
    public static boolean protect(ServerPlayerEntity player, BlockPos pos, String pin) {
        World world = player.getWorld();
        boolean ok = false;
        for (BlockPos p : linked(world, canonical(world, pos))) {
            BlockEntity be = world.getBlockEntity(p);
            if (be instanceof PasswordProtected d) {
                d.chestProtector$setProtection(pin, player.getUuid(), player.getName().getString());
                sync(world, p, be);
                ok = true;
            }
        }
        return ok;
    }
    public static void clear(World world, BlockPos pos) {
        for (BlockPos p : linked(world, canonical(world, pos))) {
            BlockEntity be = world.getBlockEntity(p);
            if (be instanceof PasswordProtected d) {
                d.chestProtector$clearProtection();
                sync(world, p, be);
            }
        }
    }

    /**
     * Persists a protection change and pushes the locked flag to observing clients.
     * {@code markDirty} alone only schedules a save; the block update is what makes
     * {@code ChunkHolder} ask the block entity for an update packet.
     */
    private static void sync(World world, BlockPos pos, BlockEntity be) {
        be.markDirty();
        BlockState state = world.getBlockState(pos);
        world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
    }
    public static void open(ServerPlayerEntity player, BlockPos pos) {
        BlockPos c=canonical(player.getWorld(),pos); NamedScreenHandlerFactory f=player.getWorld().getBlockState(c).createScreenHandlerFactory(player.getWorld(),c); if (f!=null) player.openHandledScreen(f);
    }
    /** Stable representative position of a chest, shared by both halves of a double chest. */
    public static BlockPos canonical(World world, BlockPos pos) {
        BlockPos mate = partner(world, pos);
        return mate == null || compare(pos, mate) <= 0 ? pos : mate;
    }

    /** Every position that makes up the chest at {@code pos}: one entry, or two for a double chest. */
    private static Set<BlockPos> linked(World world, BlockPos pos) {
        LinkedHashSet<BlockPos> set = new LinkedHashSet<>();
        set.add(pos);
        BlockPos mate = partner(world, pos);
        if (mate != null) set.add(mate);
        return set;
    }

    /**
     * Position of the other half of a double chest, or {@code null} if the chest is single.
     * {@link ChestBlock#getFacing(BlockState)} already resolves to the direction of the paired
     * chest, so it must not be rotated again. The neighbour is validated to guard against
     * transient block states while a double chest is being formed or split.
     */
    private static BlockPos partner(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return null;
        ChestType type = state.get(ChestBlock.CHEST_TYPE);
        if (type == ChestType.SINGLE) return null;
        BlockPos mate = pos.offset(ChestBlock.getFacing(state));
        BlockState mateState = world.getBlockState(mate);
        if (mateState.getBlock() != state.getBlock()) return null;
        if (mateState.get(ChestBlock.FACING) != state.get(ChestBlock.FACING)) return null;
        return mateState.get(ChestBlock.CHEST_TYPE) == (type == ChestType.LEFT ? ChestType.RIGHT : ChestType.LEFT) ? mate : null;
    }
    private static int compare(BlockPos a, BlockPos b) { int c=Integer.compare(a.getX(),b.getX()); if(c!=0)return c; c=Integer.compare(a.getY(),b.getY()); return c!=0?c:Integer.compare(a.getZ(),b.getZ()); }
}
