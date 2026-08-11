package com.chestprotector.backport.mixin;

import com.chestprotector.backport.PasswordProtected;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Publishes the locked state of a protected chest to clients, without ever sending the PIN.
 * <p>
 * The two methods live on {@code BlockEntity} rather than {@code ChestBlockEntity}, which
 * declares neither, so injecting keeps the inherited behaviour and any other mod's injections
 * intact instead of replacing them with a merged override. Every callback is gated on
 * {@link PasswordProtected}, so only chests are affected.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    /**
     * Used for chunk loads and, via {@code BlockEntityUpdateS2CPacket.create}, for live updates.
     * Only the non-secret fields are added; {@link PasswordProtected#PASSWORD_KEY} is omitted so
     * the PIN cannot be read off the wire.
     */
    @Inject(method = "toInitialChunkDataNbt", at = @At("RETURN"))
    private void chestProtector$addSyncedState(RegistryWrapper.WrapperLookup registryLookup, CallbackInfoReturnable<NbtCompound> cir) {
        if (!(this instanceof PasswordProtected data) || !data.chestProtector$isProtected()) return;
        NbtCompound nbt = cir.getReturnValue();
        if (nbt == null) return;
        nbt.putBoolean(PasswordProtected.LOCKED_KEY, true);
        if (data.chestProtector$getOwnerUuid() != null) nbt.putUuid(PasswordProtected.OWNER_UUID_KEY, data.chestProtector$getOwnerUuid());
        if (!data.chestProtector$getOwnerName().isEmpty()) nbt.putString(PasswordProtected.OWNER_NAME_KEY, data.chestProtector$getOwnerName());
    }

    /**
     * Returns a packet for every chest, not just locked ones. Returning {@code null} when a chest
     * is unlocked would leave clients stuck on a stale locked state after the PIN is removed,
     * because the packet that carries the cleared flag is this one.
     */
    @Inject(method = "toUpdatePacket", at = @At("RETURN"), cancellable = true)
    private void chestProtector$provideUpdatePacket(CallbackInfoReturnable<Packet<ClientPlayPacketListener>> cir) {
        if (cir.getReturnValue() == null && this instanceof PasswordProtected) {
            cir.setReturnValue(BlockEntityUpdateS2CPacket.create((BlockEntity) (Object) this));
        }
    }
}
