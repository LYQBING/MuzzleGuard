package cn.blockforge.muzzleguard;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.model.ModelPart;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public final class MuzzleGuardClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		try {
			ClassLoader loader = MuzzleGuardClient.class.getClassLoader();
			Class<?> rendererType = Class.forName("dev.emi.trinkets.api.client.TrinketRenderer", true, loader);
			Class<?> registryType = Class.forName("dev.emi.trinkets.api.client.TrinketRendererRegistry", true, loader);
			Method register = registryType.getMethod("registerRenderer", Item.class, rendererType);
			register.invoke(null, MuzzleGuardMod.MUZZLE, renderer(rendererType, true));
			register.invoke(null, MuzzleGuardMod.LOCKED_MUZZLE, renderer(rendererType, true));
			register.invoke(null, MuzzleGuardMod.COLLAR, renderer(rendererType, false));
			register.invoke(null, MuzzleGuardMod.LOCKED_COLLAR, renderer(rendererType, false));
			register.invoke(null, MuzzleGuardMod.LOCKBOX_PHOTO, renderer(rendererType, false));
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Could not register Muzzle Guard Trinket renderers", exception);
		}
	}

	private static Object renderer(Class<?> rendererType, boolean face) {
		return Proxy.newProxyInstance(rendererType.getClassLoader(), new Class<?>[]{rendererType}, (proxy, method, args) -> {
			if (method.getDeclaringClass() == Object.class) {
				return switch (method.getName()) {
					case "toString" -> "MuzzleGuardWearableRenderer";
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> null;
				};
			}
			if (method.getName().equals("render") && args != null && args.length == 9) {
				ItemStack stack = (ItemStack) args[0];
				Object model = args[2];
				MatrixStack matrices = (MatrixStack) args[3];
				OrderedRenderCommandQueue queue = (OrderedRenderCommandQueue) args[4];
				int light = (Integer) args[5];
				render(stack, model, matrices, queue, light, face);
			}
			return null;
		});
	}

	private static void render(ItemStack stack, Object model, MatrixStack matrices,
			OrderedRenderCommandQueue queue, int light, boolean face) {
		if (!(model instanceof PlayerEntityModel<?> playerModel)) return;

		ModelPart part = face ? playerModel.head : playerModel.body;
		matrices.push();
		part.applyTransform(matrices);
		matrices.translate(0.0, face ? 0.0 : -0.32, face ? -0.24 : -0.13);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
		matrices.scale(face ? 0.5F : 0.62F, face ? 0.5F : 0.42F, 0.35F);

		ItemRenderState itemState = new ItemRenderState();
		MinecraftClient.getInstance().getItemModelManager().clearAndUpdate(
				itemState, stack, ItemDisplayContext.NONE, null, null, stack.hashCode());
		itemState.render(matrices, queue, light, 0, 0);
		matrices.pop();
	}
}
