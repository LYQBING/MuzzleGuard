package cn.blockforge.muzzleguard;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.regex.Pattern;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

public final class MuzzleGuardMod implements ModInitializer {
	public static final String MOD_ID = "muzzle_guard";
	private static final String KEY_UUID = "muzzle_guard_key";
	private static final String BOUND_KEY_UUID = "muzzle_guard_bound_key";
	private static final String LOCKED = "muzzle_guard_locked";
	private static final Pattern MUFFLED_SPEECH = Pattern.compile("^[呜啊哇呀嗯哼呃哈唔哦噢诶欸哎咿嘤唉，。！？…~～,.!?、：:；;'\"“”‘’（）()\\[\\]{}\\s—-]+$");
	private static final Identifier MUZZLE_ID = Identifier.of(MOD_ID, "muzzle");
	private static final Identifier LOCKED_MUZZLE_ID = Identifier.of(MOD_ID, "locked_muzzle");
	private static final Identifier COLLAR_ID = Identifier.of(MOD_ID, "collar");
	private static final Identifier LOCKED_COLLAR_ID = Identifier.of(MOD_ID, "locked_collar");
	private static final Identifier KEY_ID = Identifier.of(MOD_ID, "key");
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
					.maxCount(1))
	);
	public static final Item LOCKED_MUZZLE = registerItem(LOCKED_MUZZLE_ID);
	public static final Item COLLAR = registerItem(COLLAR_ID);
	public static final Item LOCKED_COLLAR = registerItem(LOCKED_COLLAR_ID);
	public static final Item KEY = registerItem(KEY_ID);

	private static Item registerItem(Identifier id) {
		return Registry.register(
				Registries.ITEM,
				id,
				new Item(new Item.Settings()
						.registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
						.maxCount(1))
		);
	}

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
							entries.add(LOCKED_MUZZLE);
							entries.add(COLLAR);
							entries.add(LOCKED_COLLAR);
							entries.add(KEY);
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
		registerTrinketRules();
		registerPlayerInteractions();
		registerAnimationBlocker();
	}

	private static boolean isMuzzleEquipped(PlayerEntity player) {
		return isEquipped(player, MUZZLE) || isEquipped(player, LOCKED_MUZZLE);
	}

	private static boolean isEquipped(PlayerEntity player, Item item) {
		try {
			Class<?> api = Class.forName("dev.emi.trinkets.api.TrinketsApi");
			Object component = api.getMethod("getTrinketComponent", LivingEntity.class).invoke(null, player);
			if (component instanceof Optional<?> optional && optional.isPresent()) {
				return (boolean) Class.forName("dev.emi.trinkets.api.TrinketComponent")
						.getMethod("isEquipped", Item.class).invoke(optional.get(), item);
			}
		} catch (ReflectiveOperationException exception) {
			System.err.println("[Muzzle Guard] Failed to inspect Trinkets equipment: " + exception);
		}
		return false;
	}

	private static void registerPlayerInteractions() {
		registerFabricCallback("net.fabricmc.fabric.api.event.player.UseEntityCallback", (proxy, method, args) -> {
			if (args == null || args.length < 4 || !(args[0] instanceof PlayerEntity player)
					|| !(args[3] instanceof PlayerEntity target)) {
				return actionResult(method, "PASS");
			}
			if (player.getEntityWorld().isClient()) return actionResult(method, "PASS");
			ItemStack held = player.getStackInHand((net.minecraft.util.Hand) args[2]);
			if (held.isOf(KEY)) {
				return bindOrToggle(held, target) ? actionResult(method, "SUCCESS") : actionResult(method, "PASS");
			}
			return equipHeldItem(player, target, held) ? actionResult(method, "SUCCESS") : actionResult(method, "PASS");
		});
		registerFabricCallback("net.fabricmc.fabric.api.event.player.UseItemCallback", (proxy, method, args) -> {
			if (args == null || args.length < 3 || !(args[0] instanceof PlayerEntity player)) {
				return actionResult(method, "PASS");
			}
			if (player.getEntityWorld().isClient()) return actionResult(method, "PASS");
			ItemStack held = player.getStackInHand((net.minecraft.util.Hand) args[2]);
			if (held.isOf(KEY)) {
				return bindOrToggle(held, player) ? actionResult(method, "SUCCESS") : actionResult(method, "PASS");
			}
			return equipHeldItem(player, player, held) ? actionResult(method, "SUCCESS") : actionResult(method, "PASS");
		});
	}

	private static void registerFabricCallback(String callbackName, InvocationHandler handler) {
		try {
			ClassLoader loader = MuzzleGuardMod.class.getClassLoader();
			Class<?> callback = Class.forName(callbackName, true, loader);
			Object event = callback.getField("EVENT").get(null);
			Object listener = Proxy.newProxyInstance(callback.getClassLoader(), new Class<?>[]{callback}, handler);
			Class.forName("net.fabricmc.fabric.api.event.Event", true, loader)
					.getMethod("register", Object.class).invoke(event, listener);
		} catch (ReflectiveOperationException | LinkageError exception) {
			throw new IllegalStateException("Could not register player interaction callback " + callbackName, exception);
		}
	}

	private static Object actionResult(Method callbackMethod, String name) {
		Class<?> resultType = callbackMethod.getReturnType();
		if (resultType.getName().equals("net.minecraft.class_1269")) {
			try {
				Class<?> interactionResult = Class.forName("net.minecraft.class_1269");
				for (Object value : interactionResult.getEnumConstants()) {
					if (((Enum<?>) value).name().equals(name)) return value;
				}
			} catch (ClassNotFoundException exception) {
				throw new IllegalStateException("Could not resolve Minecraft interaction result", exception);
			}
		}
		if (resultType.isEnum()) {
			for (Object value : resultType.getEnumConstants()) {
				if (((Enum<?>) value).name().equals(name)) return value;
			}
		}
		try {
			return resultType.getField(name).get(null);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Could not resolve callback result " + resultType.getName() + "." + name, exception);
		}
	}

	private static boolean equipHeldItem(PlayerEntity player, PlayerEntity target, ItemStack held) {
		if (held.isEmpty()) return false;
		String group;
		String slot;
		if (held.isOf(MUZZLE) || held.isOf(LOCKED_MUZZLE)) {
			group = "head";
			slot = "face";
		} else if (held.isOf(COLLAR) || held.isOf(LOCKED_COLLAR)) {
			group = "chest";
			slot = "necklace";
		} else {
			return false;
		}
		try {
			Map<?, ?> groups = getTrinketInventories(target);
			Object slots = groups == null ? null : groups.get(group);
			Object inventory = slots instanceof Map<?, ?> slotMap ? slotMap.get(slot) : null;
			Class<?> inventoryApi = Class.forName("dev.emi.trinkets.api.TrinketInventory");
			if (inventory == null || (int) inventoryApi.getMethod("size").invoke(inventory) < 1) return false;
			ItemStack equipped = (ItemStack) inventoryApi.getMethod("getStack", int.class).invoke(inventory, 0);
			if (!equipped.isEmpty()) return false;
			ItemStack placed = held.copyWithCount(1);
			inventoryApi.getMethod("setStack", int.class, ItemStack.class).invoke(inventory, 0, placed);
			inventoryApi.getMethod("markUpdate", PlayerEntity.class).invoke(inventory, target);
			if (!player.getAbilities().creativeMode) held.decrement(1);
			return true;
		} catch (ReflectiveOperationException exception) {
			System.err.println("[Muzzle Guard] Failed to equip Trinket: " + exception);
			return false;
		}
	}

	private static Map<?, ?> getTrinketInventories(PlayerEntity player) throws ReflectiveOperationException {
		Class<?> api = Class.forName("dev.emi.trinkets.api.TrinketsApi");
		Object component = api.getMethod("getTrinketComponent", LivingEntity.class).invoke(null, player);
		if (component instanceof Optional<?> optional && optional.isPresent()) {
			return (Map<?, ?>) optional.get().getClass().getMethod("getInventory").invoke(optional.get());
		}
		return null;
	}

	private static boolean bindOrToggle(ItemStack key, PlayerEntity target) {
		try {
			String id = keyId(key);
			Map<?, ?> groups = getTrinketInventories(target);
			if (groups == null) return false;
			for (String[] slot : new String[][]{{"head", "face"}, {"chest", "necklace"}}) {
				Object slots = groups.get(slot[0]);
				Object inventory = slots instanceof Map<?, ?> slotMap ? slotMap.get(slot[1]) : null;
				if (inventory == null) continue;
				Class<?> inventoryApi = Class.forName("dev.emi.trinkets.api.TrinketInventory");
				int size = (int) inventoryApi.getMethod("size").invoke(inventory);
				for (int index = 0; index < size; index++) {
					ItemStack worn = (ItemStack) inventoryApi.getMethod("getStack", int.class).invoke(inventory, index);
					if (worn.isOf(LOCKED_MUZZLE) || worn.isOf(LOCKED_COLLAR)) {
						NbtCompound data = customData(worn);
						String bound = data.getString(BOUND_KEY_UUID).orElse("");
						if (bound.isEmpty()) {
							data.putString(BOUND_KEY_UUID, id);
							data.putBoolean(LOCKED, false);
							setCustomData(worn, data);
							target.sendMessage(Text.translatable("message.muzzle_guard.key_bound"), false);
						} else if (bound.equals(id)) {
							boolean locked = !data.getBoolean(LOCKED).orElse(false);
							data.putBoolean(LOCKED, locked);
							setCustomData(worn, data);
							target.sendMessage(Text.translatable(locked
									? "message.muzzle_guard.locked"
									: "message.muzzle_guard.unlocked"), false);
						} else {
							target.sendMessage(Text.translatable("message.muzzle_guard.key_mismatch"), false);
						continue;
					}
					inventoryApi.getMethod("markUpdate", PlayerEntity.class).invoke(inventory, target);
					return true;
					}
				}
			}
		} catch (ReflectiveOperationException exception) {
			System.err.println("[Muzzle Guard] Failed to bind or toggle Trinket lock: " + exception);
		}
		return false;
	}

	private static String keyId(ItemStack key) {
		NbtCompound data = customData(key);
		String id = data.getString(KEY_UUID).orElse("");
		if (id.isEmpty()) {
			id = UUID.randomUUID().toString();
			data.putString(KEY_UUID, id);
			setCustomData(key, data);
		}
		return id;
	}

	private static NbtCompound customData(ItemStack stack) {
		NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
		return component == null ? new NbtCompound() : component.copyNbt();
	}

	private static void setCustomData(ItemStack stack, NbtCompound data) {
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
	}

	private static void registerTrinketRules() {
		try {
			Class<?> api = Class.forName("dev.emi.trinkets.api.TrinketsApi");
			Class<?> trinket = Class.forName("dev.emi.trinkets.api.Trinket");
			MethodHandles.Lookup lookup = MethodHandles.lookup();
			for (Item item : new Item[]{MUZZLE, LOCKED_MUZZLE, COLLAR, LOCKED_COLLAR, LOCKBOX_PHOTO}) {
				boolean lockable = item == LOCKED_MUZZLE || item == LOCKED_COLLAR;
				Object implementation = Proxy.newProxyInstance(trinket.getClassLoader(), new Class<?>[]{trinket}, (proxy, method, args) -> {
					if (method.getName().equals("canUnequip") && args != null && args.length > 0) {
						if (!lockable) return true;
						NbtCompound data = customData((ItemStack) args[0]);
						return !data.getBoolean(LOCKED).orElse(false);
					}
					if (method.getName().equals("toString")) return "MuzzleGuardLockableTrinket";
					if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
					if (method.getName().equals("equals")) return proxy == args[0];
					if (method.isDefault()) {
						return MethodHandles.privateLookupIn(trinket, lookup)
								.findSpecial(trinket, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()), trinket)
								.bindTo(proxy).invokeWithArguments(args == null ? new Object[0] : args);
					}
					return method.getReturnType() == boolean.class;
				});
				api.getMethod("registerTrinket", Item.class, trinket).invoke(null, item, implementation);
			}
		} catch (ReflectiveOperationException | LinkageError exception) {
			throw new IllegalStateException("Could not register locked Trinkets behavior", exception);
		}
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
			return true;
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
			PlayerEntity requester = (PlayerEntity) contextClass.getMethod("requester").invoke(context);
			List<PlayerEntity> protectedPlayers = new java.util.ArrayList<>();
			@SuppressWarnings("unchecked")
			List<UUID> actors = (List<UUID>) contextClass.getMethod("actorUuids").invoke(context);
			boolean onlyPlayers = actors.size() >= 2;
			for (UUID actorId : actors) {
				PlayerEntity player = world.getPlayerByUuid(actorId);
				if (player == null) {
					onlyPlayers = false;
				} else if (isLockboxPhotoEquipped(player)) {
					protectedPlayers.add(player);
				}
			}
			if (requester != null && isLockboxPhotoEquipped(requester) && !protectedPlayers.contains(requester)) {
				protectedPlayers.add(requester);
			}
			boolean playerInitiatedPlayerAnimation = onlyPlayers
					&& requester != null
					&& actors.contains(requester.getUuid());
			if (protectedPlayers.isEmpty() || playerInitiatedPlayerAnimation) {
				return true;
			}
			int messageIndex = ThreadLocalRandom.current().nextInt(5);
			Text message = Text.translatable("message.muzzle_guard.lockbox_blocked." + messageIndex);
			for (PlayerEntity player : protectedPlayers) {
				player.sendMessage(message, false);
			}
			return false;
		} catch (ReflectiveOperationException | ClassCastException exception) {
			System.err.println("[Muzzle Guard] Failed to inspect a Needs of Nature animation: " + exception);
			return false;
		}
	}
}
