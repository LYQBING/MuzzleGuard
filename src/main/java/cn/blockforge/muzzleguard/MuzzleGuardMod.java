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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class MuzzleGuardMod implements ModInitializer {
	public static final String MOD_ID = "muzzle_guard";
	private static final Identifier MUZZLE_ID = Identifier.of(MOD_ID, "muzzle");
	public static final Item MUZZLE = Registry.register(
			Registries.ITEM,
			MUZZLE_ID,
			new Item(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, MUZZLE_ID))
					.maxCount(1)
					.equippable(EquipmentSlot.HEAD))
	);

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(MUZZLE));
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
			Class<?> eventClass = Class.forName("net.fabricmc.fabric.api.event.Event", true, loader);
			Method register = eventClass.getMethod("register", Object.class);
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

			ServerWorld world = (ServerWorld) contextClass.getMethod("world").invoke(context);
			@SuppressWarnings("unchecked")
			List<UUID> actors = (List<UUID>) contextClass.getMethod("actorUuids").invoke(context);
			for (UUID actorId : actors) {
				PlayerEntity player = world.getPlayerByUuid(actorId);
				if (player != null && player.getEquippedStack(EquipmentSlot.HEAD).isOf(MUZZLE)) {
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
