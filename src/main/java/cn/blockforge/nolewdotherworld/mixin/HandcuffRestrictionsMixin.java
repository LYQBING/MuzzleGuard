package cn.blockforge.nolewdotherworld.mixin;

import cn.blockforge.nolewdotherworld.NoLewdOtherworldMod;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
		if (!NoLewdOtherworldMod.areHandcuffsRestricting(player)) return;
		if (NoLewdOtherworldMod.isUnlockKey(player.getItemInHand(packet.getHand()))) return;
		callbackInfo.cancel();
	}

	@Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo callbackInfo) {
		if (NoLewdOtherworldMod.areHandcuffsRestricting(player)) callbackInfo.cancel();
	}

	@Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockEntityInteractions(ServerboundInteractPacket packet, CallbackInfo callbackInfo) {
		if (NoLewdOtherworldMod.areHandcuffsRestricting(player)) callbackInfo.cancel();
	}

	@Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockBlockBreaking(ServerboundPlayerActionPacket packet, CallbackInfo callbackInfo) {
		if (!NoLewdOtherworldMod.areHandcuffsRestricting(player)) return;
		ServerboundPlayerActionPacket.Action action = packet.getAction();
		if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
				|| action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK
				|| action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
				|| action == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) callbackInfo.cancel();
	}

	@Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockEquipmentChanges(ServerboundContainerClickPacket packet, CallbackInfo callbackInfo) {
		if (!NoLewdOtherworldMod.areHandcuffsRestricting(player)) return;
		int slotId = packet.getSlotNum();
		if (slotId >= 0 && slotId < player.containerMenu.slots.size()) {
			Slot slot = player.containerMenu.getSlot(slotId);
			if (isEquipmentSlot(slot, slotId) || isEquipmentQuickMove(packet, slot)) callbackInfo.cancel();
		}
	}

	@Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockCommands(ServerboundChatCommandPacket packet, CallbackInfo callbackInfo) {
		if (NoLewdOtherworldMod.areHandcuffsRestricting(player)) callbackInfo.cancel();
	}

	@Inject(method = "handleContainerButtonClick", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockContainerButtons(ServerboundContainerButtonClickPacket packet, CallbackInfo callbackInfo) {
		if (NoLewdOtherworldMod.areHandcuffsRestricting(player)) callbackInfo.cancel();
	}

	private static boolean isEquipmentSlot(Slot slot, int slotId) {
		return (slotId >= 5 && slotId <= 8) || slotId == 45 || slot.container instanceof TrinketInventory;
	}

	private static boolean isEquipmentQuickMove(ServerboundContainerClickPacket packet, Slot slot) {
		if (packet.getClickType() != ClickType.QUICK_MOVE) return false;
		ItemStack stack = slot.getItem();
		return stack.has(DataComponents.EQUIPPABLE) || stack.has(TrinketDataComponents.EQUIPMENT);
	}
}
