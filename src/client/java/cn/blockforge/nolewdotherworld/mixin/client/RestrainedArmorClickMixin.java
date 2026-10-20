package cn.blockforge.nolewdotherworld.mixin.client;

import cn.blockforge.nolewdotherworld.NoLewdOtherworldMod;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class RestrainedArmorClickMixin {
	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockArmorAutoEquip(Player player, InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> callbackInfo) {
		if (!NoLewdOtherworldMod.isRestrainedByStraitjacket(player)) return;
		if (!isArmorItem(player.getItemInHand(hand))) return;
		player.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.restrained_armor"));
		callbackInfo.setReturnValue(InteractionResult.FAIL);
	}

	private static boolean isArmorItem(net.minecraft.world.item.ItemStack stack) {
		var equippable = stack.get(DataComponents.EQUIPPABLE);
		return equippable != null && (equippable.slot() == EquipmentSlot.HEAD
				|| equippable.slot() == EquipmentSlot.CHEST
				|| equippable.slot() == EquipmentSlot.LEGS
				|| equippable.slot() == EquipmentSlot.FEET);
	}
}
