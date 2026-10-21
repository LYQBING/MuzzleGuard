package cn.blockforge.nolewdotherworld.mixin;

import cn.blockforge.nolewdotherworld.HandcuffRestrictionService;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class HandcuffCommandMixin {
	@Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockCommandsWhileHandcuffed(ServerboundChatCommandPacket packet, CallbackInfo callbackInfo) {
		ServerGamePacketListenerImpl listener = (ServerGamePacketListenerImpl) (Object) this;
		if (HandcuffRestrictionService.isHandcuffed(listener.player)) callbackInfo.cancel();
	}
}