package cn.blockforge.nolewdotherworld.mixin;

import cn.blockforge.nolewdotherworld.NoLewdOtherworldMod;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
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

	@Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockEntityInteractions(ServerboundInteractPacket packet, CallbackInfo callbackInfo) {
		if (!NoLewdOtherworldMod.isRestrainedByStraitjacket(player)) return;
		boolean[] attacking = {false};
		packet.dispatch(new ServerboundInteractPacket.Handler() {
			@Override
			public void onInteraction(net.minecraft.world.InteractionHand hand) {
			}

			@Override
			public void onInteraction(net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.Vec3 location) {
			}

			@Override
			public void onAttack() {
				attacking[0] = true;
			}
		});
		if (!attacking[0]) return;
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.no_lewd_otherworld.restrained_attack"));
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
		if (slotId == -999 && cursor.is(NoLewdOtherworldMod.LOCKED_STRAITJACKET)) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.no_lewd_otherworld.restrained_unequip"));
			callbackInfo.cancel();
			return;
		}
		if (slotId >= 0 && slotId < player.containerMenu.slots.size()) {
			ItemStack clicked = player.containerMenu.getSlot(slotId).getItem();
			if (clicked.is(NoLewdOtherworldMod.LOCKED_STRAITJACKET)) {
				player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.no_lewd_otherworld.restrained_unequip"));
				callbackInfo.cancel();
				return;
			}
		}
		if (packet.slotNum() >= 0 && packet.slotNum() < player.containerMenu.slots.size()) {
			if (cursor.is(NoLewdOtherworldMod.LOCKED_STRAITJACKET)) {
				player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.no_lewd_otherworld.restrained_unequip"));
				callbackInfo.cancel();
			}
		}
	}

	@Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockCommands(ServerboundChatCommandPacket packet, CallbackInfo callbackInfo) {
		if (!NoLewdOtherworldMod.isRestrainedByStraitjacket(player)) return;
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.no_lewd_otherworld.restrained_command"));
		callbackInfo.cancel();
	}
}
