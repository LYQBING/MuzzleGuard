package cn.blockforge.muzzleguard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import eu.pb4.trinkets.api.component.TrinketEquippable;
import eu.pb4.trinkets.api.event.TrinketCanUnequipCallback;
import dev.yumi.commons.TriState;

public final class MuzzleGuardMod implements ModInitializer {
	public static final String MOD_ID = "muzzle_guard";
	private static final String KEY_UUID = "muzzle_guard_key";
	private static final String BOUND_KEY_UUID = "muzzle_guard_bound_key";
	private static final String COLLAR_UUID = "muzzle_guard_collar";
	private static final String CONTROLLER_PLAYER_UUID = "muzzle_guard_controller_player";
	private static final String CONTROLLER_COLLAR_UUID = "muzzle_guard_controller_collar";
	private static final String LOCKED = "muzzle_guard_locked";
	private static final String[] MUFFLED_SYLLABLES = {"呜", "啊", "哇", "呀", "嗯", "哼", "唔", "哦", "噢", "诶", "欸", "哎", "咿", "嘤", "喵"};
	private static final Identifier MUZZLE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "muzzle");
	private static final Identifier MUZZLE_RENDER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "muzzle_render");
	private static final Identifier LOCKED_MUZZLE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "locked_muzzle");
	private static final Identifier COLLAR_ID = Identifier.fromNamespaceAndPath(MOD_ID, "collar");
	private static final Identifier LOCKED_COLLAR_ID = Identifier.fromNamespaceAndPath(MOD_ID, "locked_collar");
	private static final Identifier KEY_ID = Identifier.fromNamespaceAndPath(MOD_ID, "key");
	private static final Identifier MASTER_KEY_ID = Identifier.fromNamespaceAndPath(MOD_ID, "master_key");
	private static final Identifier SHOCK_CONTROLLER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shock_controller");
	private static final Identifier LOCKBOX_PHOTO_ID = Identifier.fromNamespaceAndPath(MOD_ID, "lockbox_photo");
	public static final Item LOCKBOX_PHOTO = Registry.register(
			BuiltInRegistries.ITEM,
			LOCKBOX_PHOTO_ID,
			new Item(new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, LOCKBOX_PHOTO_ID))
					.component(TrinketDataComponents.EQUIPMENT, TrinketEquippable.DEFAULT.withSlots("chest/necklace"))
					.stacksTo(1))
	);
	public static final Item SHOCK_CONTROLLER = Registry.register(
			BuiltInRegistries.ITEM,
			SHOCK_CONTROLLER_ID,
			new ShockControllerItem(new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, SHOCK_CONTROLLER_ID))
					.stacksTo(1))
	);
	public static final Item MUZZLE = Registry.register(
			BuiltInRegistries.ITEM,
			MUZZLE_ID,
			new WearableItem(new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, MUZZLE_ID))
					.component(TrinketDataComponents.EQUIPMENT, TrinketEquippable.DEFAULT.withSlots("head/face"))
					.stacksTo(1), "head", "face")
	);
	public static final Item MUZZLE_RENDER = Registry.register(
			BuiltInRegistries.ITEM,
			MUZZLE_RENDER_ID,
			new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MUZZLE_RENDER_ID)).stacksTo(1))
	);
	public static final Item LOCKED_MUZZLE = registerWearable(LOCKED_MUZZLE_ID, "head", "face");
	public static final Item COLLAR = registerWearable(COLLAR_ID, "chest", "necklace");
	public static final Item LOCKED_COLLAR = registerWearable(LOCKED_COLLAR_ID, "chest", "necklace");
	public static final Item KEY = Registry.register(
			BuiltInRegistries.ITEM,
			KEY_ID,
			new KeyItem(new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, KEY_ID))
					.stacksTo(1))
	);
	public static final Item MASTER_KEY = Registry.register(
			BuiltInRegistries.ITEM,
			MASTER_KEY_ID,
			new MasterKeyItem(new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, MASTER_KEY_ID))
					.stacksTo(1))
	);

	private static Item registerWearable(Identifier id, String group, String slot) {
		return Registry.register(
				BuiltInRegistries.ITEM,
				id,
				new WearableItem(new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, id))
						.component(TrinketDataComponents.EQUIPMENT,
								TrinketEquippable.DEFAULT.withSlots(group + "/" + slot))
						.stacksTo(1), group, slot)
		);
	}

	@Override
	public void onInitialize() {
		Registry.register(
				BuiltInRegistries.CREATIVE_MODE_TAB,
				Identifier.fromNamespaceAndPath(MOD_ID, "main"),
				CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable("itemGroup.muzzle_guard"))
						.icon(() -> new ItemStack(MUZZLE))
						.displayItems((context, entries) -> {
							entries.accept(MUZZLE);
							entries.accept(LOCKED_MUZZLE);
							entries.accept(COLLAR);
							entries.accept(LOCKED_COLLAR);
							entries.accept(KEY);
							entries.accept(MASTER_KEY);
							entries.accept(SHOCK_CONTROLLER);
							entries.accept(LOCKBOX_PHOTO);
						})
						.build()
		);
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
			if (!isMuzzleEquipped(sender)) {
				return true;
			}

			String content = message.signedContent();
			int[] codePoints = content.codePoints().toArray();
			boolean onlyMuffledSpeech = true;
			for (int codePoint : codePoints) {
				if (Character.isLetterOrDigit(codePoint) && !isMuffledSyllable(codePoint)) {
					onlyMuffledSpeech = false;
					break;
				}
			}
			if (onlyMuffledSpeech) return true;

			StringBuilder muffledContent = new StringBuilder();
			for (int codePoint : codePoints) {
				if (Character.isLetterOrDigit(codePoint) && !isMuffledSyllable(codePoint)) {
					muffledContent.append(MUFFLED_SYLLABLES[ThreadLocalRandom.current().nextInt(MUFFLED_SYLLABLES.length)]);
				} else {
					muffledContent.appendCodePoint(codePoint);
				}
			}
			Component muffledMessage = sender.getDisplayName().copy().append(Component.literal(": ")).append(Component.literal(muffledContent.toString()));
			sender.level().getServer().getPlayerList().broadcastSystemMessage(muffledMessage, false);
			return false;
		});
		registerTrinketRules();
		registerAnimationBlocker();
	}

	private static boolean isMuffledSyllable(int codePoint) {
		for (String syllable : MUFFLED_SYLLABLES) {
			if (syllable.codePointAt(0) == codePoint) return true;
		}
		return false;
	}

	private static boolean removeLockBinding(Player actor, Player target) {
		TrinketAttachment attachment = TrinketsApi.getAttachment(target);
		for (String slot : new String[]{"head/face", "chest/necklace"}) {
			TrinketInventory inventory = attachment.getInventory(slot);
			if (inventory == null) continue;
			for (int index = 0; index < inventory.getContainerSize(); index++) {
				ItemStack worn = inventory.getItem(index);
				if (worn.isEmpty() || !(worn.is(LOCKED_COLLAR) || worn.is(LOCKED_MUZZLE))) continue;
				CompoundTag data = customData(worn);
				data.remove(BOUND_KEY_UUID);
				data.remove(LOCKED);
				setCustomData(worn, data);
				inventory.setChanged();
				Component result = Component.translatable("message.muzzle_guard.master_key_unlocked");
				actor.sendSystemMessage(result);
				if (!actor.getUUID().equals(target.getUUID())) target.sendSystemMessage(result);
				return true;
			}
		}
		actor.sendSystemMessage(Component.translatable("message.muzzle_guard.key_no_target"));
		return false;
	}

	private static final class WearableItem extends Item {
		private final String slot;

		private WearableItem(Item.Properties settings, String group, String slot) {
			super(settings);
			this.slot = group + "/" + slot;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> textConsumer, TooltipFlag flag) {
			super.appendHoverText(stack, context, display, textConsumer, flag);
			if (this != LOCKED_MUZZLE && this != LOCKED_COLLAR) return;

			CompoundTag data = customData(stack);
			String boundKey = data.getString(BOUND_KEY_UUID).orElse("");
			textConsumer.accept(Component.translatable(boundKey.isEmpty()
					? "tooltip.muzzle_guard.unbound"
					: "tooltip.muzzle_guard.bound_key", shortId(boundKey)));
			if (!boundKey.isEmpty()) {
				textConsumer.accept(Component.translatable(data.getBoolean(LOCKED).orElse(false)
						? "tooltip.muzzle_guard.locked"
						: "tooltip.muzzle_guard.unlocked"));
			}
		}

		@Override
		public InteractionResult use(Level level, Player user, InteractionHand hand) {
			ItemStack stack = user.getItemInHand(hand);
			if (!level.isClientSide()) {
				if (equipHeldItem(user, user, stack, slot)) {
					user.sendSystemMessage(Component.translatable("message.muzzle_guard.equipped_self"));
				} else {
					user.sendSystemMessage(Component.translatable("message.muzzle_guard.equip_failed"));
				}
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
			if (!user.level().isClientSide() && entity instanceof Player target) {
				ItemStack held = user.getItemInHand(hand);
				if (equipHeldItem(user, target, held, slot)) {
					Component message = Component.translatable("message.muzzle_guard.equipped_other", target.getDisplayName());
					user.sendSystemMessage(message);
					if (!user.getUUID().equals(target.getUUID())) {
						target.sendSystemMessage(Component.translatable("message.muzzle_guard.equipped_by", user.getDisplayName()));
					}
				} else {
					user.sendSystemMessage(Component.translatable("message.muzzle_guard.equip_failed"));
				}
			}
			return InteractionResult.SUCCESS;
		}
	}

	private static final class KeyItem extends Item {
		private KeyItem(Item.Properties settings) {
			super(settings);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> textConsumer, TooltipFlag flag) {
			super.appendHoverText(stack, context, display, textConsumer, flag);
			String id = customData(stack).getString(KEY_UUID).orElse("");
			if (!id.isEmpty()) textConsumer.accept(Component.translatable("tooltip.muzzle_guard.key_id", shortId(id)));
		}

		@Override
		public InteractionResult use(Level level, Player user, InteractionHand hand) {
			ItemStack stack = user.getItemInHand(hand);
			if (!level.isClientSide()) bindOrToggle(stack, user, user);
			return InteractionResult.SUCCESS;
		}

		@Override
		public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
			if (!user.level().isClientSide() && entity instanceof Player target) {
				bindOrToggle(user.getItemInHand(hand), user, target);
			}
			return InteractionResult.SUCCESS;
		}
	}

	private static final class MasterKeyItem extends Item {
		private MasterKeyItem(Item.Properties settings) {
			super(settings);
		}

		@Override
		public InteractionResult use(Level level, Player user, InteractionHand hand) {
			if (!level.isClientSide() && removeLockBinding(user, user)) consumeIfSurvival(user, hand);
			return InteractionResult.SUCCESS;
		}

		@Override
		public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
			if (!user.level().isClientSide() && entity instanceof Player target
					&& removeLockBinding(user, target)) {
				consumeIfSurvival(user, hand);
			}
			return InteractionResult.SUCCESS;
		}

		private static void consumeIfSurvival(Player user, InteractionHand hand) {
			if (!user.getAbilities().instabuild) user.getItemInHand(hand).shrink(1);
		}
	}

	private static final class ShockControllerItem extends Item {
		private ShockControllerItem(Item.Properties settings) {
			super(settings);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> textConsumer, TooltipFlag flag) {
			super.appendHoverText(stack, context, display, textConsumer, flag);
			CompoundTag data = customData(stack);
			String playerId = data.getString(CONTROLLER_PLAYER_UUID).orElse("");
			String collarId = data.getString(CONTROLLER_COLLAR_UUID).orElse("");
			if (!playerId.isEmpty() && !collarId.isEmpty()) {
				textConsumer.accept(Component.translatable("tooltip.muzzle_guard.controller_bound", shortId(playerId), shortId(collarId)));
			}
		}

		@Override
		public InteractionResult use(Level level, Player user, InteractionHand hand) {
			if (!level.isClientSide()) useController(user.getItemInHand(hand), user, user);
			return InteractionResult.SUCCESS;
		}

		@Override
		public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
			if (!user.level().isClientSide() && entity instanceof Player target) {
				useController(user.getItemInHand(hand), user, target);
			}
			return InteractionResult.SUCCESS;
		}
	}

	private static void useController(ItemStack controller, Player user, Player selectedPlayer) {
		CompoundTag controllerData = customData(controller);
		String playerId = controllerData.getString(CONTROLLER_PLAYER_UUID).orElse("");
		String collarId = controllerData.getString(CONTROLLER_COLLAR_UUID).orElse("");
		if (playerId.isEmpty() || collarId.isEmpty()) {
			ItemStack collar = getEquippedCollar(selectedPlayer);
			if (collar == null) {
				user.sendSystemMessage(Component.translatable("message.muzzle_guard.controller_no_collar"));
				return;
			}
			if (collar.is(LOCKED_COLLAR) && customData(collar).getBoolean(LOCKED).orElse(false)
					&& !hasMatchingKey(user, customData(collar).getString(BOUND_KEY_UUID).orElse(""))) {
				user.sendSystemMessage(Component.translatable("message.muzzle_guard.controller_collar_locked"));
				return;
			}
			collarId = getOrCreateCollarId(collar);
			markNecklaceInventoryUpdated(selectedPlayer);
			controllerData.putString(CONTROLLER_PLAYER_UUID, selectedPlayer.getStringUUID());
			controllerData.putString(CONTROLLER_COLLAR_UUID, collarId);
			setCustomData(controller, controllerData);
			user.sendSystemMessage(Component.translatable("message.muzzle_guard.controller_bound", selectedPlayer.getDisplayName(), shortId(collarId)));
			return;
		}

		Player target;
		try {
			target = user.level().getServer().getPlayerList().getPlayer(UUID.fromString(playerId));
		} catch (IllegalArgumentException exception) {
			target = null;
		}
		if (target == null || !collarId.equals(getEquippedCollarId(target))) {
			controllerData.remove(CONTROLLER_PLAYER_UUID);
			controllerData.remove(CONTROLLER_COLLAR_UUID);
			setCustomData(controller, controllerData);
			user.sendSystemMessage(Component.translatable("message.muzzle_guard.controller_unbound"));
			return;
		}

		target.hurtServer((ServerLevel) target.level(), user.damageSources().magic(), 1.0F);
		user.sendSystemMessage(Component.translatable("message.muzzle_guard.controller_shocked", target.getDisplayName()));
		if (!user.getUUID().equals(target.getUUID())) {
			target.sendSystemMessage(Component.translatable("message.muzzle_guard.controller_shocked_target"));
		}
	}

	private static boolean hasMatchingKey(Player player, String boundKeyId) {
		if (boundKeyId.isEmpty()) return false;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(KEY) && boundKeyId.equals(customData(stack).getString(KEY_UUID).orElse(""))) return true;
		}
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(KEY) && boundKeyId.equals(customData(stack).getString(KEY_UUID).orElse(""))) return true;
		}
		return false;
	}

	private static ItemStack getEquippedCollar(Player player) {
		TrinketAttachment attachment = TrinketsApi.getAttachment(player);
		TrinketInventory inventory = attachment.getInventory("chest/necklace");
		if (inventory == null) return null;
		for (int index = 0; index < inventory.getContainerSize(); index++) {
			ItemStack stack = inventory.getItem(index);
			if (stack.is(COLLAR) || stack.is(LOCKED_COLLAR)) return stack;
		}
		return null;
	}

	private static String getEquippedCollarId(Player player) {
		ItemStack collar = getEquippedCollar(player);
		return collar == null ? "" : customData(collar).getString(COLLAR_UUID).orElse("");
	}

	private static String getOrCreateCollarId(ItemStack collar) {
		CompoundTag data = customData(collar);
		String id = data.getString(COLLAR_UUID).orElse("");
		if (id.isEmpty()) {
			id = UUID.randomUUID().toString();
			data.putString(COLLAR_UUID, id);
			setCustomData(collar, data);
		}
		return id;
	}

	private static void markNecklaceInventoryUpdated(Player player) {
		TrinketInventory inventory = TrinketsApi.getAttachment(player).getInventory("chest/necklace");
		if (inventory != null) inventory.setChanged();
	}

	private static boolean isMuzzleEquipped(Player player) {
		return isEquipped(player, MUZZLE) || isEquipped(player, LOCKED_MUZZLE);
	}

	private static boolean isEquipped(Player player, Item item) {
		return TrinketsApi.getAttachment(player).isEquipped(item);
	}

	private static boolean equipHeldItem(Player player, Player target, ItemStack held, String slot) {
		if (held.isEmpty()) return false;
		TrinketInventory inventory = TrinketsApi.getAttachment(target).getInventory(slot);
		if (inventory == null || inventory.getContainerSize() < 1 || !inventory.getItem(0).isEmpty()) return false;
		inventory.setItem(0, held.copyWithCount(1));
		inventory.setChanged();
		if (!player.getAbilities().instabuild) {
			held.shrink(1);
			player.getInventory().setChanged();
		}
		return true;
	}

	private static TrinketInventory getTrinketInventory(Player player, String slot) {
		return TrinketsApi.getAttachment(player).getInventory(slot);
	}

	private static boolean bindOrToggle(ItemStack key, Player actor, Player target) {
		String id = customData(key).getString(KEY_UUID).orElse("");
		String boundKeyId = id;
		List<LockCandidate> candidates = new ArrayList<>();
		for (String slot : new String[]{"head/face", "chest/necklace"}) {
			TrinketInventory inventory = getTrinketInventory(target, slot);
			if (inventory == null) continue;
			for (int index = 0; index < inventory.getContainerSize(); index++) {
				ItemStack worn = inventory.getItem(index);
				if (isLockableTrinket(worn)) candidates.add(new LockCandidate(worn, inventory));
			}
		}
		LockCandidate selected = boundKeyId.isEmpty() ? null : candidates.stream()
				.filter(candidate -> candidate.boundKey.equals(boundKeyId))
				.findFirst().orElse(null);
		if (selected == null) {
			selected = candidates.stream().filter(candidate -> candidate.boundKey.isEmpty()).findFirst().orElse(null);
		}
		if (selected == null) {
			if (candidates.isEmpty()) {
				actor.sendSystemMessage(Component.translatable("message.muzzle_guard.key_no_target"));
			} else {
				actor.sendSystemMessage(Component.translatable("message.muzzle_guard.key_bound_elsewhere", candidates.get(0).stack.getHoverName()));
			}
			return false;
		}
		boolean newlyBound = selected.boundKey.isEmpty();
		if (newlyBound) {
			id = id.isEmpty() ? keyId(key) : id;
			selected.data.putString(BOUND_KEY_UUID, id);
			selected.data.putBoolean(LOCKED, true);
		} else {
			selected.data.putBoolean(LOCKED, !selected.data.getBoolean(LOCKED).orElse(false));
		}
		setCustomData(selected.stack, selected.data);
		selected.inventory.setChanged();
		String message = newlyBound
				? "message.muzzle_guard.key_bound_to"
				: selected.data.getBoolean(LOCKED).orElse(false)
						? "message.muzzle_guard.locked_item"
						: "message.muzzle_guard.unlocked_item";
		Component result = Component.translatable(message, selected.stack.getHoverName());
		actor.sendSystemMessage(result);
		if (!actor.getUUID().equals(target.getUUID())) target.sendSystemMessage(result);
		return true;
	}

	private static boolean isLockableTrinket(ItemStack stack) {
		return !stack.isEmpty() && (stack.is(LOCKED_MUZZLE) || stack.is(LOCKED_COLLAR));
	}

	private static final class LockCandidate {
		private final ItemStack stack;
		private final TrinketInventory inventory;
		private final CompoundTag data;
		private final String boundKey;

		private LockCandidate(ItemStack stack, TrinketInventory inventory) {
			this.stack = stack;
			this.inventory = inventory;
			this.data = customData(stack);
			this.boundKey = data.getString(BOUND_KEY_UUID).orElse("");
		}
	}

	private static String keyId(ItemStack key) {
		CompoundTag data = customData(key);
		String id = data.getString(KEY_UUID).orElse("");
		if (id.isEmpty()) {
			id = UUID.randomUUID().toString();
			data.putString(KEY_UUID, id);
			setCustomData(key, data);
		}
		return id;
	}

	private static String shortId(String id) {
		return id.length() <= 8 ? id : id.substring(0, 8);
	}

	private static CompoundTag customData(ItemStack stack) {
		CustomData component = stack.get(DataComponents.CUSTOM_DATA);
		return component == null ? new CompoundTag() : component.copyTag();
	}

	private static void setCustomData(ItemStack stack, CompoundTag data) {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
	}

	private static void registerTrinketRules() {
		// Register rule to prevent unequipping when locked
		TrinketCanUnequipCallback.EVENT.register((stack, slot, entity, canUnequipDefault) -> {
			if (stack.is(LOCKED_MUZZLE) || stack.is(LOCKED_COLLAR)) {
				return customData(stack).getBoolean(LOCKED).orElse(false) ? TriState.FALSE : TriState.DEFAULT;
			}
			return TriState.DEFAULT;
		});
	}

	private static boolean isLockboxPhotoEquipped(Player player) {
		return TrinketsApi.getAttachment(player).isEquipped(LOCKBOX_PHOTO);
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
			ServerLevel world = (ServerLevel) contextClass.getMethod("world").invoke(context);
			Player requester = (Player) contextClass.getMethod("requester").invoke(context);
			List<Player> protectedPlayers = new java.util.ArrayList<>();
			@SuppressWarnings("unchecked")
			List<UUID> actors = (List<UUID>) contextClass.getMethod("actorUuids").invoke(context);
			boolean onlyPlayers = actors.size() >= 2;
			for (UUID actorId : actors) {
				Player player = world.getPlayerByUUID(actorId);
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
					&& actors.contains(requester.getUUID());
			if (protectedPlayers.isEmpty() || playerInitiatedPlayerAnimation) {
				return true;
			}
			return false;
		} catch (ReflectiveOperationException | ClassCastException exception) {
			System.err.println("[Muzzle Guard] Failed to inspect a Needs of Nature animation: " + exception);
			return false;
		}
	}
}
