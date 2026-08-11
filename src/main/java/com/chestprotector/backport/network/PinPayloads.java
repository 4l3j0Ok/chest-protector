package com.chestprotector.backport.network;

import com.chestprotector.backport.ChestProtectorBackport;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class PinPayloads {
    private PinPayloads() {}

    public record OpenSetup(BlockPos pos) implements CustomPayload {
        public static final Id<OpenSetup> ID = new Id<>(Identifier.of(ChestProtectorBackport.MOD_ID, "open_setup"));
        public static final PacketCodec<RegistryByteBuf, OpenSetup> CODEC = CustomPayload.codecOf(OpenSetup::write, OpenSetup::new);
        private OpenSetup(RegistryByteBuf buf) { this(buf.readBlockPos()); }
        private void write(RegistryByteBuf buf) { buf.writeBlockPos(pos); }
        public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record OpenUnlock(BlockPos pos) implements CustomPayload {
        public static final Id<OpenUnlock> ID = new Id<>(Identifier.of(ChestProtectorBackport.MOD_ID, "open_unlock"));
        public static final PacketCodec<RegistryByteBuf, OpenUnlock> CODEC = CustomPayload.codecOf(OpenUnlock::write, OpenUnlock::new);
        private OpenUnlock(RegistryByteBuf buf) { this(buf.readBlockPos()); }
        private void write(RegistryByteBuf buf) { buf.writeBlockPos(pos); }
        public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record SubmitSetup(BlockPos pos, String pin) implements CustomPayload {
        public static final Id<SubmitSetup> ID = new Id<>(Identifier.of(ChestProtectorBackport.MOD_ID, "submit_setup"));
        public static final PacketCodec<RegistryByteBuf, SubmitSetup> CODEC = CustomPayload.codecOf(SubmitSetup::write, SubmitSetup::new);
        private SubmitSetup(RegistryByteBuf buf) { this(buf.readBlockPos(), buf.readString(6)); }
        private void write(RegistryByteBuf buf) { buf.writeBlockPos(pos); buf.writeString(pin, 6); }
        public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record SubmitUnlock(BlockPos pos, String pin) implements CustomPayload {
        public static final Id<SubmitUnlock> ID = new Id<>(Identifier.of(ChestProtectorBackport.MOD_ID, "submit_unlock"));
        public static final PacketCodec<RegistryByteBuf, SubmitUnlock> CODEC = CustomPayload.codecOf(SubmitUnlock::write, SubmitUnlock::new);
        private SubmitUnlock(RegistryByteBuf buf) { this(buf.readBlockPos(), buf.readString(6)); }
        private void write(RegistryByteBuf buf) { buf.writeBlockPos(pos); buf.writeString(pin, 6); }
        public Id<? extends CustomPayload> getId() { return ID; }
    }
}
