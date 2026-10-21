package cn.blockforge.nolewdotherworld.mixin.client;

import cn.blockforge.nolewdotherworld.WearableTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class HandcuffPoseMixin {
	@Shadow
	public ModelPart rightArm;
	@Shadow
	public ModelPart leftArm;

	@Inject(method = "setupAnim", at = @At("TAIL"))
	private void noLewdOtherworld$poseHandcuffedArms(AvatarRenderState state, CallbackInfo callbackInfo) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) return;
		Entity entity = minecraft.level.getEntity(state.id);
		if (!(entity instanceof Player player) || !WearableTags.isHandcuffed(player)) return;

		rightArm.xRot = -1.25F;
		leftArm.xRot = -1.25F;
		rightArm.yRot = -0.15F;
		leftArm.yRot = 0.15F;
	}
}
