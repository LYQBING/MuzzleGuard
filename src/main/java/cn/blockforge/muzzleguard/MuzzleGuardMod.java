package cn.blockforge.muzzleguard;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class MuzzleGuardMod implements ModInitializer {
	public static final String MOD_ID = "muzzle_guard";
	public static final Item MUZZLE = Registry.register(
			BuiltInRegistries.ITEM,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "muzzle"),
			new Item(new Item.Properties().stacksTo(1).equippable(EquipmentSlot.HEAD))
	);

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> entries.accept(MUZZLE));
		registerAnimationBlocker();
	}

	private static void registerAnimationBlocker() {
		try {
			ClassLoader loader = MuzzleGuardMod.class.getClassLoader();
			Class<?> eventsClass = Class.forName(
					"com.nonid.internal.animation.api.AnimationLifecycleEvents",
					true,
					loader
			);
			Class<?> callbackClass = Class.forName(
					"com.nonid.internal.animation.api.AnimationLifecycleEvents$AllowStart",
					true,
					loader
			);
			Field allowStartField = eventsClass.getField("ALLOW_START");
			Object event = allowStartField.get(null);
			Object callback = Proxy.newProxyInstance(
					callbackClass.getClassLoader(),
					new Class<?>[]{callbackClass},
					MuzzleGuardMod::invokeCallback
			);
			Method register = event.getClass().getMethod("register", callbackClass);
			register.invoke(event, callback);
		} catch (ReflectiveOperationException | LinkageError exception) {
			throw new IllegalStateException("Could not register the Needs of Nature animation blocker", exception);
		}
	}

	private static Object invokeCallback(Object proxy, Method method, Object[] arguments) {
		if (method.getDeclaringClass() == Object.class) {
			return switch (method.getName()) {
				case "toString" -> "MuzzleGuardAllowStartListener";
				case "hashCode" -> System.identityHashCode(proxy);
				case "equals" -> proxy == arguments[0];
				default -> null;
			};
		}
		if (!method.getName().equals("allowStart") || arguments == null || arguments.length != 1) {
			return true;
		}
		return allowStart(arguments[0]);
	}

	private static boolean allowStart(Object context) {
		try {
			Class<?> contextClass = context.getClass();
			Object animationId = contextClass.getMethod("animationId").invoke(context);
			Object actorKeys = contextClass.getMethod("actorKeys").invoke(context);
			if (!isMouthAnimation(String.valueOf(animationId), actorKeys)) {
				return true;
			}

			ServerLevel world = (ServerLevel) contextClass.getMethod("world").invoke(context);
			@SuppressWarnings("unchecked")
			List<UUID> actors = (List<UUID>) contextClass.getMethod("actorUuids").invoke(context);
			for (UUID actorId : actors) {
				ServerPlayer player = world.getServer().getPlayerList().getPlayer(actorId);
				if (player != null && player.getItemBySlot(EquipmentSlot.HEAD).is(MUZZLE)) {
					return false;
				}
			}
		} catch (ReflectiveOperationException | ClassCastException exception) {
			System.err.println("[Muzzle Guard] Failed to inspect a Needs of Nature animation: " + exception);
		}
		return true;
	}

	private static boolean isMouthAnimation(String animationId, Object actorKeys) {
		String searchable = animationId.toLowerCase(Locale.ROOT) + " " + String.valueOf(actorKeys).toLowerCase(Locale.ROOT);
		return searchable.contains("oral")
				|| searchable.contains("mouth")
				|| searchable.contains("kiss")
				|| searchable.contains("fellatio")
				|| searchable.contains("blowjob")
				|| searchable.contains("suck");
	}
}
