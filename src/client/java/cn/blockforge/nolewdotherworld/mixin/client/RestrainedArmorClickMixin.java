package cn.blockforge.nolewdotherworld.mixin.client;

import cn.blockforge.nolewdotherworld.NoLewdOtherworldMod;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class RestrainedArmorClickMixin {
	@Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockRestrictedArmorClick(int containerId, int slotId, int button,
			@Coerce Object clickType, Player player, CallbackInfo callbackInfo) {
		if (!NoLewdOtherworldMod.isRestrainedByStraitjacket(player)) return;
		if (player.containerMenu.containerId != containerId) return;

		boolean blocked = false;
		boolean removingStraitjacket = false;
		if (slotId >= 0 && slotId < player.containerMenu.slots.size()) {
			var slot = player.containerMenu.getSlot(slotId);
			ItemStack clicked = slot.getItem();
			boolean armorItem = isArmorItem(clicked);
			boolean armorSlot = slot instanceof ArmorSlot;
			removingStraitjacket = clicked.is(NoLewdOtherworldMod.LOCKED_STRAITJACKET)
					|| player.containerMenu.getCarried().is(NoLewdOtherworldMod.LOCKED_STRAITJACKET);
			String clickMode = clickType instanceof Enum<?> clickEnum ? clickEnum.name() : "";
			blocked = armorSlot || removingStraitjacket
					|| (armorItem && ("QUICK_MOVE".equals(clickMode)
							|| ("PICKUP".equals(clickMode) && button == 1)));
		} else if (slotId == -999) {
			blocked = player.containerMenu.getCarried().is(NoLewdOtherworldMod.LOCKED_STRAITJACKET);
			removingStraitjacket = blocked;
		}

		if (blocked) {
			player.sendSystemMessage(Component.translatable(removingStraitjacket
					? "message.no_lewd_otherworld.restrained_unequip"
					: "message.no_lewd_otherworld.restrained_armor"));
			callbackInfo.cancel();
		}
	}

	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockArmorAutoEquip(Player player, InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> callbackInfo) {
		if (!NoLewdOtherworldMod.isRestrainedByStraitjacket(player)) return;
		if (!isArmorItem(player.getItemInHand(hand))) return;
		player.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.restrained_armor"));
		callbackInfo.setReturnValue(InteractionResult.FAIL);
	}

	private static boolean isArmorItem(ItemStack stack) {
		var equippable = stack.get(DataComponents.EQUIPPABLE);
		return equippable != null && (equippable.slot() == EquipmentSlot.HEAD
				|| equippable.slot() == EquipmentSlot.CHEST
				|| equippable.slot() == EquipmentSlot.LEGS
				|| equippable.slot() == EquipmentSlot.FEET);
	}
}
