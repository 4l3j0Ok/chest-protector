package com.chestprotector.backport.mixin;

import com.chestprotector.backport.ChestProtectorBackport;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    /**
     * Hides protected chests from hopper transfers.
     * <p>
     * {@code getBlockInventoryAt} is the single point both hopper directions funnel through:
     * {@code getOutputInventory} reaches it via {@code getInventoryAt(World, BlockPos)} while
     * {@code getInputInventory} reaches it via the private {@code getInventoryAt} overload that
     * takes coordinates. Injecting here therefore covers insertion and extraction alike.
     * <p>
     * The returned inventory is deliberately not type-checked: a double chest yields a
     * {@code DoubleInventory} rather than a {@code ChestBlockEntity}, so an {@code instanceof}
     * test on the chest block entity would let hoppers drain protected double chests.
     */
    @Inject(method = "getBlockInventoryAt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/inventory/Inventory;",
            at = @At("RETURN"), cancellable = true)
    private static void chestProtector$blockProtectedInventory(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<Inventory> cir) {
        if (cir.getReturnValue() != null && ChestProtectorBackport.isProtected(world, pos)) {
            cir.setReturnValue(null);
        }
    }
}
