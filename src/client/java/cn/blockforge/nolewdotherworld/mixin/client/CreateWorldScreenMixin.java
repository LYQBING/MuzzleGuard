package cn.blockforge.nolewdotherworld.mixin.client;

import cn.blockforge.nolewdotherworld.WorldCreationGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
	private static final Component BLOCKED_MESSAGE = Component.literal("当前环境不满足世界创建的基本要求");

	@Inject(method = "createLevel", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$checkWorldCreationPermission(CallbackInfo callbackInfo) {
		if (!WorldCreationGate.isAllowed()) {
			Minecraft minecraft = Minecraft.getInstance();
			SystemToast.add(minecraft.getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, BLOCKED_MESSAGE, Component.empty());
			callbackInfo.cancel();
		}
	}
}
