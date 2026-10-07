package cn.blockforge.muzzleguard;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.regex.Pattern;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

public final class MuzzleGuardMod implements ModInitializer {
	public static final String MOD_ID = "muzzle_guard";
	private static final Pattern MUFFLED_SPEECH = Pattern.compile("^[呜啊哇呀嗯哼呃哈唔哦噢诶欸哎咿嘤唉，。！？…~～,.!?、：:；;'\"“”‘’（）()\\[\\]{}\\s—-]+$");
	private static final Identifier MUZZLE_ID = Identifier.of(MOD_ID, "muzzle");
	private static final RegistryKey<EquipmentAsset> MUZZLE_EQUIPMENT =
			RegistryKey.of(
					EquipmentAssetKeys.IRON.getRegistryRef(),
					MUZZLE_ID
			);
	private static final Identifier LOCKBOX_PHOTO_ID = Identifier.of(MOD_ID, "lockbox_photo");
	public static final Item LOCKBOX_PHOTO = Registry.register(
			Registries.ITEM,
			LOCKBOX_PHOTO_ID,
			new Item(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, LOCKBOX_PHOTO_ID))
					.maxCount(1))
	);
	public static final Item MUZZLE = Registry.register(
			Registries.ITEM,
			MUZZLE_ID,
			new Item(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, MUZZLE_ID))
					.maxCount(1)
					.component(DataComponentTypes.EQUIPPABLE,
							EquippableComponent.builder(EquipmentSlot.HEAD).model(MUZZLE_EQUIPMENT).build()))
	);

	@Override
	public void onInitialize() {
		Registry.register(
				Registries.ITEM_GROUP,
				Identifier.of(MOD_ID, "main"),
				FabricItemGroup.builder()
						.displayName(Text.translatable("itemGroup.muzzle_guard"))
						.icon(() -> new ItemStack(MUZZLE))
						.entries((context, entries) -> {
							entries.add(MUZZLE);
							entries.add(LOCKBOX_PHOTO);
						})
						.build()
		);
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
			if (!isMuzzleEquipped(sender)) {
				return true;
			}
			String content = message.getContent().getString();
			if (MUFFLED_SPEECH.matcher(content).matches() && content.matches(".*[呜啊哇呀嗯哼呃哈唔哦噢诶欸哎咿嘤唉].*")) {
				return true;
			}

			sender.sendMessage(Text.translatable("message.muzzle_guard.muffled"));
			Text muffledMessage = sender.getDisplayName().copy().append(Text.literal(": 呜呜呜"));
			sender.getEntityWorld().getServer().getPlayerManager().broadcast(
					muffledMessage,
					recipient -> muffledMessage,
					false
			);
			return false;
		});
		registerAnimationBlocker();
	}

	private static boolean isMuzzleEquipped(PlayerEntity player) {
		return player.getEquippedStack(EquipmentSlot.HEAD).isOf(MUZZLE);
	}

	private static boolean isLockboxPhotoEquipped(PlayerEntity player) {
		try {
			Class<?> trinketsApi = Class.forName("dev.emi.trinkets.api.TrinketsApi");
			Object component = trinketsApi.getMethod("getTrinketComponent", LivingEntity.class).invoke(null, player);
			if (component instanceof Optional<?> optional && optional.isPresent()) {
				Class<?> componentClass = Class.forName("dev.emi.trinkets.api.TrinketComponent");
				return (boolean) componentClass.getMethod("isEquipped", Item.class).invoke(optional.get(), LOCKBOX_PHOTO);
			}
		} catch (ReflectiveOperationException exception) {
			System.err.println("[Muzzle Guard] Failed to inspect the Trinkets necklace slot: " + exception);
		}
		return false;
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
			ServerWorld world = (ServerWorld) contextClass.getMethod("world").invoke(context);
			List<PlayerEntity> protectedPlayers = new java.util.ArrayList<>();
			@SuppressWarnings("unchecked")
			List<UUID> actors = (List<UUID>) contextClass.getMethod("actorUuids").invoke(context);
			for (UUID actorId : actors) {
				PlayerEntity player = world.getPlayerByUuid(actorId);
				if (player != null && isLockboxPhotoEquipped(player)) {
					protectedPlayers.add(player);
				}
			}
			PlayerEntity requester = (PlayerEntity) contextClass.getMethod("requester").invoke(context);
			if (requester != null && isLockboxPhotoEquipped(requester) && !protectedPlayers.contains(requester)) {
				protectedPlayers.add(requester);
			}
			if (!protectedPlayers.isEmpty()) {
				int messageIndex = ThreadLocalRandom.current().nextInt(5);
				Text message = Text.translatable("message.muzzle_guard.lockbox_blocked." + messageIndex);
				for (PlayerEntity player : protectedPlayers) {
					player.sendMessage(message, false);
				}
				return false;
			}
		} catch (ReflectiveOperationException | ClassCastException exception) {
			System.err.println("[Muzzle Guard] Failed to inspect a Needs of Nature animation: " + exception);
		}
		return true;
	}
}
