package cn.blockforge.nolewdotherworld.mixin;

import cn.blockforge.nolewdotherworld.NoLewdOtherworldMod;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
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
		for (int i = 0; i < player.containerMenu.slots.size(); i++) {
			if (!player.containerMenu.getSlot(i).getItem().is(NoLewdOtherworldMod.LOCKED_STRAITJACKET)) continue;
			if (slotId == i || packet.changedSlots().containsKey(i)) {
				affectsStraitjacket = true;
				break;
			}
		}
		if (affectsStraitjacket) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.no_lewd_otherworld.restrained_unequip"));
			player.containerMenu.broadcastFullState();
			callbackInfo.cancel();
		}
	}

	@Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockCommands(ServerboundChatCommandPacket packet, CallbackInfo callbackInfo) {
		if (!NoLewdOtherworldMod.isRestrainedByStraitjacket(player)) return;
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.no_lewd_otherworld.restrained_command"));
		callbackInfo.cancel();
	}
}
