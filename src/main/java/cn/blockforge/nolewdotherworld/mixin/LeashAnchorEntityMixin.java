package cn.blockforge.nolewdotherworld.mixin;

import cn.blockforge.nolewdotherworld.PlayerLeashManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class LeashAnchorEntityMixin {
	@Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$makeLeashAnchorUnpickable(CallbackInfoReturnable<Boolean> callbackInfo) {
		if (PlayerLeashManager.isLeashAnchor((Entity) (Object) this)) callbackInfo.setReturnValue(false);
	}

	@Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
	private void noLewdOtherworld$removeLeashAnchorCollisionBox(CallbackInfoReturnable<AABB> callbackInfo) {
		Entity entity = (Entity) (Object) this;
		if (PlayerLeashManager.isLeashAnchor(entity)) {
			double x = entity.getX();
			double y = entity.getY();
			double z = entity.getZ();
			callbackInfo.setReturnValue(new AABB(x, y, z, x, y, z));
		}
	}
}