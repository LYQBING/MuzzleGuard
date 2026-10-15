package cn.blockforge.nolewdotherworld.mixin.client;

import cn.blockforge.nolewdotherworld.WorldCreationGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public abstract class ClientIntegratedServerMixin {
	private static final Component BLOCKED_MESSAGE = Component.literal("当前环境不满足世界创建的基本要求");

	@Inject(method = "initServer", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$checkIntegratedServerStartupPermission(CallbackInfoReturnable<Boolean> callbackInfo) {
		if (!WorldCreationGate.isAllowed()) {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.execute(() -> SystemToast.add(
					minecraft.getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
					BLOCKED_MESSAGE, Component.empty()));
			callbackInfo.setReturnValue(false);
		}
	}
}
