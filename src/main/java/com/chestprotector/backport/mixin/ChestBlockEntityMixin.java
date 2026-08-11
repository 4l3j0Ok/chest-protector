package com.chestprotector.backport.mixin;

import com.chestprotector.backport.PasswordProtected;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin implements PasswordProtected {
    /** Never leaves the server: excluded from {@code toInitialChunkDataNbt}. */
    @Unique private String chestProtector$password = "";
    /**
     * Mirrors "a PIN is set". Kept as its own field because clients receive this flag but never
     * the PIN, so they cannot derive the locked state from {@link #chestProtector$password}.
     */
    @Unique private boolean chestProtector$locked;
    @Unique private UUID chestProtector$ownerUuid;
    @Unique private String chestProtector$ownerName = "";

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void chestProtector$writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfo ci) {
        if (!chestProtector$password.isEmpty()) nbt.putString(PASSWORD_KEY, chestProtector$password);
        if (chestProtector$ownerUuid != null) nbt.putUuid(OWNER_UUID_KEY, chestProtector$ownerUuid);
        if (!chestProtector$ownerName.isEmpty()) nbt.putString(OWNER_NAME_KEY, chestProtector$ownerName);
    }

    /**
     * Handles both sources of data. On disk the PIN is present and the locked state is derived
     * from it; {@link LOCKED_KEY} is intentionally never written to disk. From a sync packet the
     * PIN is absent and only the flag is present.
     */
    @Inject(method = "readNbt", at = @At("TAIL"))
    private void chestProtector$readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfo ci) {
        chestProtector$password = nbt.getString(PASSWORD_KEY);
        chestProtector$locked = !chestProtector$password.isEmpty() || nbt.getBoolean(LOCKED_KEY);
        chestProtector$ownerUuid = nbt.containsUuid(OWNER_UUID_KEY) ? nbt.getUuid(OWNER_UUID_KEY) : null;
        chestProtector$ownerName = nbt.getString(OWNER_NAME_KEY);
    }

    public boolean chestProtector$isProtected() { return chestProtector$locked; }
    public String chestProtector$getPassword() { return chestProtector$password; }
    public void chestProtector$setProtection(String password, UUID ownerUuid, String ownerName) {
        chestProtector$password = password == null ? "" : password;
        chestProtector$locked = !chestProtector$password.isEmpty();
        chestProtector$ownerUuid = ownerUuid;
        chestProtector$ownerName = ownerName == null ? "" : ownerName;
    }
    public void chestProtector$clearProtection() {
        chestProtector$password = "";
        chestProtector$locked = false;
        chestProtector$ownerUuid = null;
        chestProtector$ownerName = "";
    }
    public UUID chestProtector$getOwnerUuid() { return chestProtector$ownerUuid; }
    public String chestProtector$getOwnerName() { return chestProtector$ownerName; }
}
