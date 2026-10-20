package cn.blockforge.nolewdotherworld.mixin;

import cn.blockforge.nolewdotherworld.PlayerLeashManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class LeashAnchorCollisionMixin {
	@Inject(method = "push", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$preventLeashAnchorPush(Entity other, CallbackInfo callbackInfo) {
		Entity self = (Entity) (Object) this;
		if (PlayerLeashManager.isLeashAnchor(self) || PlayerLeashManager.isLeashAnchor(other)) {
			callbackInfo.cancel();
		}
	}
}
