package com.chestprotector.mixin;

import com.chestprotector.ChestProtector;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps explosions from destroying protected chests.
 * <p>
 * Owner-only breaking is enforced through {@code PlayerBlockBreakEvents.BEFORE}, which only covers
 * players. TNT and creepers never go through it, so without this any player could blow a protected
 * chest open and collect the drops.
 * <p>
 * The filter runs on {@code Explosion} rather than {@code ExplosionBehavior.canDestroyBlock}
 * because {@code EntityExplosionBehavior} and {@code AdvancedExplosionBehavior} both override that
 * method; injecting here is downstream of every behaviour and so cannot be bypassed by one of them.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private World world;

    @Inject(method = "collectBlocksAndDamageEntities", at = @At("TAIL"))
    private void chestProtector$spareProtectedChests(CallbackInfo ci) {
        // Runs before affectWorld consumes the list, and getAffectedBlocks exposes it directly.
        ((Explosion) (Object) this).getAffectedBlocks()
            .removeIf(pos -> ChestProtector.isProtected(world, pos));
    }
}
