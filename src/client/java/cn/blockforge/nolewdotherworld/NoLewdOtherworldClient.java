package cn.blockforge.nolewdotherworld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.fabricmc.api.ClientModInitializer;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;

public final class NoLewdOtherworldClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TrinketRendererRegistry.registerRenderer(NoLewdOtherworldMod.MUZZLE, renderer(true));
		TrinketRendererRegistry.registerRenderer(NoLewdOtherworldMod.LOCKED_MUZZLE, renderer(true));
		TrinketRendererRegistry.registerRenderer(NoLewdOtherworldMod.COLLAR, renderer(false));
		TrinketRendererRegistry.registerRenderer(NoLewdOtherworldMod.LOCKED_COLLAR, renderer(false));
	}

	private static TrinketRenderer renderer(boolean face) {
		return (stack, slot, model, poseStack, submit, light, state, limbAngle, limbDistance) -> {
			if (!(model instanceof PlayerModel playerModel)) return;
			ModelPart part = face ? playerModel.head : playerModel.body;
			poseStack.pushPose();
			part.translateAndRotate(poseStack);
			poseStack.translate(0.0F, face ? -0.0625F : 0.0F, face ? -0.24F : -0.13F);
			poseStack.mulPose(new Matrix4f().rotation(Axis.YP.rotationDegrees(180.0F)));
			poseStack.scale(face ? 0.5F : 0.62F, face ? 0.5F : 0.42F, 0.35F);

			ItemStackRenderState itemState = new ItemStackRenderState();
			Minecraft.getInstance().getItemModelResolver().updateForTopItem(
					itemState, stack,
					ItemDisplayContext.NONE, Minecraft.getInstance().level, null, stack.hashCode());
			itemState.submit(poseStack, submit, light, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		};
	}
}
