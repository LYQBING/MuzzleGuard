package cn.blockforge.nolewdotherworld.mixin.client;

import cn.blockforge.nolewdotherworld.WorldCreationGate;
import cn.blockforge.nolewdotherworld.WorldCreationBlockedScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class BlockSingleplayerScreenMixin {
	@Inject(method = "setScreenAndShow", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$blockSingleplayerScreen(Screen screen, CallbackInfo callbackInfo) {
		if (!WorldCreationGate.isAllowed() && screen instanceof SelectWorldScreen) {
			Minecraft minecraft = (Minecraft) (Object) this;
			minecraft.setScreenAndShow(new WorldCreationBlockedScreen());
			callbackInfo.cancel();
		}
	}
}
