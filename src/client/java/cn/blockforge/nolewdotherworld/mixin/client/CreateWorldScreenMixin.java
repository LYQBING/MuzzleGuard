package cn.blockforge.nolewdotherworld.mixin.client;

import cn.blockforge.nolewdotherworld.WorldCreationGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
	@Unique
	private boolean noLewdOtherworld$showBlockedMessage;
	private static final Component BLOCKED_MESSAGE = Component.literal("当前环境不满足世界创建的基本要求");

	@Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$checkWorldCreationPermission(CallbackInfo callbackInfo) {
		if (!WorldCreationGate.isAllowed()) {
			noLewdOtherworld$showBlockedMessage = true;
			callbackInfo.cancel();
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void noLewdOtherworld$renderBlockedMessage(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
		if (noLewdOtherworld$showBlockedMessage) {
			Screen screen = (Screen) (Object) this;
			graphics.centeredText(Minecraft.getInstance().font, BLOCKED_MESSAGE, screen.width / 2, screen.height / 2, 0xFFFFFFFF);
		}
	}
}
