package cn.blockforge.nolewdotherworld.mixin.client;

import cn.blockforge.nolewdotherworld.WorldCreationGate;
import cn.blockforge.nolewdotherworld.WorldCreationBlockedScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class BlockSingleplayerScreenMixin {
	@Inject(method = "lambda$createNormalMenuOptions$0", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockSingleplayerScreen(Button button, CallbackInfo callbackInfo) {
		if (!WorldCreationGate.isAllowed()) {
			Minecraft.getInstance().setScreenAndShow(new WorldCreationBlockedScreen());
			callbackInfo.cancel();
		}
	}
}
