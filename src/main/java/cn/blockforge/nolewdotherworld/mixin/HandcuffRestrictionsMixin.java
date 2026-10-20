package cn.blockforge.nolewdotherworld.mixin;

import cn.blockforge.nolewdotherworld.NoLewdOtherworldMod;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class HandcuffRestrictionsMixin {
	@Shadow public ServerPlayer player;

	@Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockUseItem(ServerboundUseItemPacket packet, CallbackInfo callbackInfo) {
		if (!NoLewdOtherworldMod.isRestrainedByStraitjacket(player)) return;
		if (!NoLewdOtherworldMod.isUnlockKey(player.getItemInHand(packet.hand()))) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.no_lewd_otherworld.restrained_use"));
			callbackInfo.cancel();
		}
	}

	@Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo callbackInfo) {
		if (!NoLewdOtherworldMod.isRestrainedByStraitjacket(player)) return;
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.no_lewd_otherworld.restrained_use"));
		callbackInfo.cancel();
	}

	@Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockBlockBreaking(ServerboundPlayerActionPacket packet, CallbackInfo callbackInfo) {
		if (!NoLewdOtherworldMod.isRestrainedByStraitjacket(player)) return;
		ServerboundPlayerActionPacket.Action action = packet.getAction();
		if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
				|| action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK
				|| action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
			if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
				player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.no_lewd_otherworld.restrained_break"));
			}
			callbackInfo.cancel();
		}
	}

	@Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockStraitjacketRemoval(ServerboundContainerClickPacket packet, CallbackInfo callbackInfo) {
		if (!NoLewdOtherworldMod.isRestrainedByStraitjacket(player)) return;
		int slotId = packet.slotNum();
		ItemStack cursor = player.containerMenu.getCarried();
		boolean affectsStraitjacket = cursor.is(NoLewdOtherworldMod.LOCKED_STRAITJACKET);
		boolean affectsArmor = false;
		for (int i = 0; i < player.containerMenu.slots.size(); i++) {
			var slot = player.containerMenu.getSlot(i);
			boolean affected = slotId == i || packet.changedSlots().containsKey(i);
			if (affected && slot instanceof ArmorSlot) affectsArmor = true;
			if (affected && slot.getItem().is(NoLewdOtherworldMod.LOCKED_STRAITJACKET)) {
				affectsStraitjacket = true;
			}
		}
		ItemStack clicked = slotId >= 0 && slotId < player.containerMenu.slots.size()
				? player.containerMenu.getSlot(slotId).getItem() : ItemStack.EMPTY;
		if ((isArmorItem(clicked) && (packet.clickType() == ClickType.QUICK_MOVE
				|| (packet.clickType() == ClickType.PICKUP && packet.buttonNum() == 1)))
				|| (isArmorItem(cursor) && slotId >= 0 && slotId < player.containerMenu.slots.size()
						&& player.containerMenu.getSlot(slotId) instanceof ArmorSlot)) {
			affectsArmor = true;
		}
		if (affectsStraitjacket || affectsArmor) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(affectsStraitjacket
					? "message.no_lewd_otherworld.restrained_unequip"
					: "message.no_lewd_otherworld.restrained_armor"));
			player.containerMenu.broadcastFullState();
			callbackInfo.cancel();
		}
	}

	private static boolean isArmorItem(ItemStack stack) {
		var equippable = stack.get(DataComponents.EQUIPPABLE);
		return equippable != null && equippable.slot().isArmor();
	}

	@Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockCommands(ServerboundChatCommandPacket packet, CallbackInfo callbackInfo) {
		if (!NoLewdOtherworldMod.isRestrainedByStraitjacket(player)) return;
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.no_lewd_otherworld.restrained_command"));
		callbackInfo.cancel();
	}
}
