package com.chestprotector;

import java.util.UUID;

public interface PasswordProtected {
    /** Disk-only key. The PIN is never included in anything sent to a client. */
    String PASSWORD_KEY = "chest_protector_password";
    /** Client-facing flag: tells the client a chest is locked without revealing the PIN. */
    String LOCKED_KEY = "chest_protector_locked";
    String OWNER_UUID_KEY = "chest_protector_owner_uuid";
    String OWNER_NAME_KEY = "chest_protector_owner_name";

    boolean chestProtector$isProtected();
    String chestProtector$getPassword();
    void chestProtector$setProtection(String password, UUID ownerUuid, String ownerName);
    void chestProtector$clearProtection();
    UUID chestProtector$getOwnerUuid();
    String chestProtector$getOwnerName();
    default boolean chestProtector$isOwner(net.minecraft.entity.player.PlayerEntity player) {
        return chestProtector$getOwnerUuid() != null && chestProtector$getOwnerUuid().equals(player.getUuid());
    }
}
