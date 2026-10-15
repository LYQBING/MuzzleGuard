package cn.blockforge.nolewdotherworld.mixin;

import cn.blockforge.nolewdotherworld.WorldCreationGate;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {
	private static final String BLOCKED_MESSAGE = "当前环境不满足世界创建的基本要求";

	@Inject(method = "initServer", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$checkServerStartupPermission(CallbackInfoReturnable<Boolean> callbackInfo) {
		if (!WorldCreationGate.isAllowed()) {
			System.err.println(BLOCKED_MESSAGE);
			callbackInfo.setReturnValue(false);
		}
	}
}
