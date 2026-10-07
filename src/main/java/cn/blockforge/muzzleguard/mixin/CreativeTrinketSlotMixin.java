package cn.blockforge.muzzleguard.mixin;

import java.lang.reflect.Field;

import cn.blockforge.muzzleguard.MuzzleGuardMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "dev.emi.trinkets.CreativeTrinketSlot", remap = false)
public abstract class CreativeTrinketSlotMixin {
	public boolean method_7674(PlayerEntity player) {
		ItemStack stack = ((Slot) (Object) this).getStack();
		if (MuzzleGuardMod.isNecklaceTrinket(stack)) {
			boolean allowed = MuzzleGuardMod.canUnequipNecklace(stack);
			System.out.println("[Muzzle Guard] Creative slot take check: item="
					+ Registries.ITEM.getId(stack.getItem()) + ", allowed=" + allowed);
			return allowed;
		}
		return muzzle_guard$canTakeOriginal(player);
	}

	@Unique
	private boolean muzzle_guard$canTakeOriginal(PlayerEntity player) {
		try {
			Field original = this.getClass().getDeclaredField("original");
			original.setAccessible(true);
			return ((Slot) original.get(this)).canTakeItems(player);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Could not delegate Creative Trinket slot take check", exception);
		}
	}
}
