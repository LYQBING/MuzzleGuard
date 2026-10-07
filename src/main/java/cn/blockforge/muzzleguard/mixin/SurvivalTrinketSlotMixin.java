package cn.blockforge.muzzleguard.mixin;

import cn.blockforge.muzzleguard.MuzzleGuardMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.emi.trinkets.SurvivalTrinketSlot", remap = false)
public abstract class SurvivalTrinketSlotMixin {
	@Inject(method = "method_7674", at = @At("HEAD"), cancellable = true, remap = false)
	private void muzzle_guard$canTakeNecklace(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		ItemStack stack = ((Slot) (Object) this).getStack();
		if (MuzzleGuardMod.isNecklaceTrinket(stack)) {
			boolean allowed = MuzzleGuardMod.canUnequipNecklace(stack);
			System.out.println("[Muzzle Guard] Survival slot take check: item="
					+ Registries.ITEM.getId(stack.getItem()) + ", allowed=" + allowed);
			cir.setReturnValue(allowed);
		}
	}
}
