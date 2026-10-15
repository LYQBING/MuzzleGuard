package cn.blockforge.nolewdotherworld.mixin.client;

import cn.blockforge.nolewdotherworld.WorldCreationGate;
import cn.blockforge.nolewdotherworld.WorldCreationToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
	@Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$checkWorldCreationPermission(CallbackInfo callbackInfo) {
		if (!WorldCreationGate.isAllowed()) {
			Minecraft minecraft = Minecraft.getInstance();
			WorldCreationToast.show(minecraft);
			callbackInfo.cancel();
		}
	}
}
