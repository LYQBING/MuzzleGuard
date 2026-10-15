package cn.blockforge.nolewdotherworld.mixin.client;

import cn.blockforge.nolewdotherworld.WorldCreationGate;
import cn.blockforge.nolewdotherworld.WorldCreationToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public abstract class ClientIntegratedServerMixin {
	@Inject(method = "initServer", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$checkIntegratedServerStartupPermission(CallbackInfoReturnable<Boolean> callbackInfo) {
		if (!WorldCreationGate.isAllowed()) {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.execute(() -> WorldCreationToast.show(minecraft));
			callbackInfo.setReturnValue(false);
		}
	}
}
