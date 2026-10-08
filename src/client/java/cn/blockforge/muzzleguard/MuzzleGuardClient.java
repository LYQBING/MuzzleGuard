package cn.blockforge.muzzleguard;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.model.ModelPart;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;

public final class MuzzleGuardClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TrinketRendererRegistry.registerRenderer(MuzzleGuardMod.MUZZLE, new WearableRenderer(true));
		TrinketRendererRegistry.registerRenderer(MuzzleGuardMod.LOCKED_MUZZLE, new WearableRenderer(true));
		TrinketRendererRegistry.registerRenderer(MuzzleGuardMod.COLLAR, new WearableRenderer(false));
		TrinketRendererRegistry.registerRenderer(MuzzleGuardMod.LOCKED_COLLAR, new WearableRenderer(false));
		TrinketRendererRegistry.registerRenderer(MuzzleGuardMod.LOCKBOX_PHOTO, new WearableRenderer(false));
	}

	private static final class WearableRenderer implements TrinketRenderer {
		private final boolean face;

		private WearableRenderer(boolean face) {
			this.face = face;
		}

		@Override
		public void render(ItemStack stack, SlotReference slot, EntityModel<? extends LivingEntityRenderState> model,
				MatrixStack matrices, OrderedRenderCommandQueue queue, int light, LivingEntityRenderState state,
				float limbAngle, float limbDistance) {
			if (!(model instanceof PlayerEntityModel<?> playerModel)) return;

			ModelPart part = this.face ? playerModel.head : playerModel.body;
			matrices.push();
			part.applyTransform(matrices);
			if (this.face) {
				matrices.translate(0.0, 0.0, -0.24);
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
			} else {
				matrices.translate(0.0, -0.32, -0.105);
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
			}
			matrices.scale(this.face ? 0.48F : 0.62F, this.face ? 0.48F : 0.42F, 0.35F);

			ItemRenderState itemState = new ItemRenderState();
			MinecraftClient.getInstance().getItemModelManager().clearAndUpdate(
					itemState, stack, this.face ? ItemDisplayContext.HEAD : ItemDisplayContext.FIXED,
					null, null, stack.hashCode());
			itemState.render(matrices, queue, light, 0, 0);
			matrices.pop();
		}
	}
}
