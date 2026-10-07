package cn.blockforge.muzzleguard;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
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
	private static final Identifier COLLAR_KEY_ID = Identifier.of(MOD_ID, "collar_key");
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
			new WearableItem(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, MUZZLE_ID))
					.maxCount(1), "head", "face")
	);
	public static final Item LOCKED_MUZZLE = registerWearable(LOCKED_MUZZLE_ID, "head", "face");
	public static final Item COLLAR = registerWearable(COLLAR_ID, "chest", "necklace");
	public static final Item LOCKED_COLLAR = registerWearable(LOCKED_COLLAR_ID, "chest", "necklace");
	public static final Item KEY = Registry.register(
			Registries.ITEM,
			KEY_ID,
			new KeyItem(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, KEY_ID))
					.maxCount(1), false)
	);
	public static final Item COLLAR_KEY = Registry.register(
			Registries.ITEM,
			COLLAR_KEY_ID,
			new KeyItem(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, COLLAR_KEY_ID))
					.maxCount(1), true)
	);

	private static Item registerWearable(Identifier id, String group, String slot) {
		return Registry.register(
				Registries.ITEM,
				id,
				new WearableItem(new Item.Settings()
						.registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
						.maxCount(1), group, slot)
		);
	}

	private static Object invokeApiMethod(Class<?> api, Object receiver, String[] names, Class<?>[] parameterTypes, Object... args)
			throws ReflectiveOperationException {
		NoSuchMethodException missing = null;
		for (String name : names) {
			try {
				return api.getMethod(name, parameterTypes).invoke(receiver, args);
			} catch (NoSuchMethodException exception) {
				missing = exception;
			}
		}
		throw missing == null ? new NoSuchMethodException(api.getName()) : missing;
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
							entries.add(COLLAR_KEY);
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
		registerAnimationBlocker();
	}

	private static final class WearableItem extends Item {
		private final String group;
		private final String slot;

		private WearableItem(Settings settings, String group, String slot) {
			super(settings);
			this.group = group;
			this.slot = slot;
		}

		@Override
		public ActionResult use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!world.isClient()) equipHeldItem(user, user, stack, group, slot);
			return ActionResult.SUCCESS;
		}

		@Override
		public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
			if (!user.getEntityWorld().isClient() && entity instanceof PlayerEntity target) {
				equipHeldItem(user, target, stack, group, slot);
			}
			return ActionResult.SUCCESS;
		}
	}

	private static final class KeyItem extends Item {
		private final boolean collarKey;

		private KeyItem(Settings settings, boolean collarKey) {
			super(settings);
			this.collarKey = collarKey;
		}

		@Override
		public ActionResult use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!world.isClient()) bindOrToggle(stack, user, collarKey);
			return ActionResult.SUCCESS;
		}

		@Override
		public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
			if (!user.getEntityWorld().isClient() && entity instanceof PlayerEntity target) bindOrToggle(stack, target, collarKey);
			return ActionResult.SUCCESS;
		}
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

	private static boolean equipHeldItem(PlayerEntity player, PlayerEntity target, ItemStack held, String group, String slot) {
		if (held.isEmpty()) return false;
		try {
			Map<?, ?> groups = getTrinketInventories(target);
			Object slots = groups == null ? null : groups.get(group);
			Object inventory = slots instanceof Map<?, ?> slotMap ? slotMap.get(slot) : null;
			Class<?> inventoryApi = Class.forName("dev.emi.trinkets.api.TrinketInventory");
			if (inventory == null || ((Number) invokeApiMethod(inventoryApi, inventory,
					new String[]{"size", "method_5439"}, new Class<?>[0])).intValue() < 1) return false;
			ItemStack equipped = (ItemStack) invokeApiMethod(inventoryApi, inventory,
					new String[]{"getStack", "method_5438"}, new Class<?>[]{int.class}, 0);
			if (!equipped.isEmpty()) return false;
			ItemStack placed = held.copyWithCount(1);
			invokeApiMethod(inventoryApi, inventory, new String[]{"setStack", "method_5447"},
					new Class<?>[]{int.class, ItemStack.class}, 0, placed);
			inventoryApi.getMethod("markUpdate").invoke(inventory);
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
			Class<?> componentApi = Class.forName("dev.emi.trinkets.api.TrinketComponent");
			return (Map<?, ?>) componentApi.getMethod("getInventory").invoke(optional.get());
		}
		return null;
	}

	private static boolean bindOrToggle(ItemStack key, PlayerEntity target, boolean collarKey) {
		try {
			Map<?, ?> groups = getTrinketInventories(target);
			if (groups == null) {
				target.sendMessage(Text.translatable("message.muzzle_guard.key_no_trinkets"), false);
				return false;
			}
			String group = collarKey ? "chest" : "head";
			String slot = collarKey ? "necklace" : "face";
			Object slots = groups.get(group);
			Object inventory = slots instanceof Map<?, ?> slotMap ? slotMap.get(slot) : null;
			if (inventory == null) {
				target.sendMessage(Text.translatable("message.muzzle_guard.key_no_target"), false);
				return false;
			}
			Class<?> inventoryApi = Class.forName("dev.emi.trinkets.api.TrinketInventory");
			int size = ((Number) invokeApiMethod(inventoryApi, inventory,
					new String[]{"size", "method_5439"}, new Class<?>[0])).intValue();
			for (int index = 0; index < size; index++) {
				ItemStack worn = (ItemStack) invokeApiMethod(inventoryApi, inventory,
						new String[]{"getStack", "method_5438"}, new Class<?>[]{int.class}, index);
				boolean matching = collarKey ? worn.isOf(LOCKED_COLLAR) : worn.isOf(LOCKED_MUZZLE);
				if (!matching) continue;
				String id = keyId(key);
				NbtCompound data = customData(worn);
				String bound = data.getString(BOUND_KEY_UUID).orElse("");
				if (bound.isEmpty()) {
					data.putString(BOUND_KEY_UUID, id);
					data.putBoolean(LOCKED, true);
				} else if (bound.equals(id)) {
					boolean locked = !data.getBoolean(LOCKED).orElse(false);
					data.putBoolean(LOCKED, locked);
				} else {
					target.sendMessage(Text.translatable("message.muzzle_guard.key_mismatch"), false);
					return false;
				}
				setCustomData(worn, data);
				inventoryApi.getMethod("markUpdate").invoke(inventory);
				String message = bound.isEmpty()
						? "message.muzzle_guard.key_bound_locked"
						: data.getBoolean(LOCKED).orElse(false)
								? "message.muzzle_guard.locked"
								: "message.muzzle_guard.unlocked";
				target.sendMessage(Text.translatable(message), false);
				return true;
			}
			target.sendMessage(Text.translatable("message.muzzle_guard.key_no_target"), false);
		} catch (ReflectiveOperationException exception) {
			System.err.println("[Muzzle Guard] Failed to bind or toggle Trinket lock:");
			exception.printStackTrace(System.err);
			target.sendMessage(Text.translatable("message.muzzle_guard.key_error"), false);
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
			for (Item item : new Item[]{MUZZLE, LOCKED_MUZZLE, COLLAR, LOCKED_COLLAR, LOCKBOX_PHOTO}) {
				boolean lockable = item == LOCKED_MUZZLE || item == LOCKED_COLLAR;
				Object implementation = Proxy.newProxyInstance(trinket.getClassLoader(), new Class<?>[]{trinket}, (proxy, method, args) -> {
					if (method.getName().equals("canUnequip")) {
						if (!lockable) return Boolean.TRUE;
						if (args != null) {
							for (Object argument : args) {
								if (argument instanceof ItemStack stack) {
									NbtCompound data = customData(stack);
									return !data.getBoolean(LOCKED).orElse(false);
								}
							}
						}
						return Boolean.FALSE;
					}
					if (method.getName().equals("toString")) return "MuzzleGuardLockableTrinket";
					if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
					if (method.getName().equals("equals")) return proxy == args[0];
					return method.isDefault()
							? InvocationHandler.invokeDefault(proxy, method, args == null ? new Object[0] : args)
							: null;
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
