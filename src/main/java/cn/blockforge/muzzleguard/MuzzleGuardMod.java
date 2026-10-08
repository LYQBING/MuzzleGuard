package cn.blockforge.muzzleguard;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.function.Consumer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public final class MuzzleGuardMod implements ModInitializer {
	public static final String MOD_ID = "muzzle_guard";
	private static final String KEY_UUID = "muzzle_guard_key";
	private static final String BOUND_KEY_UUID = "muzzle_guard_bound_key";
	private static final String COLLAR_UUID = "muzzle_guard_collar";
	private static final String CONTROLLER_PLAYER_UUID = "muzzle_guard_controller_player";
	private static final String CONTROLLER_COLLAR_UUID = "muzzle_guard_controller_collar";
	private static final String LOCKED = "muzzle_guard_locked";
	private static final String LEASH_HOLDER_PREFIX = "muzzle_guard:leash_holder:";
	private static final String[] MUFFLED_SYLLABLES = {"呜", "啊", "哇", "呀", "嗯", "哼", "唔", "哦", "噢", "诶", "欸", "哎", "咿", "嘤", "喵"};
	private static final Identifier MUZZLE_ID = Identifier.of(MOD_ID, "muzzle");
	private static final Identifier MUZZLE_RENDER_ID = Identifier.of(MOD_ID, "muzzle_render");
	private static final Identifier LOCKED_MUZZLE_ID = Identifier.of(MOD_ID, "locked_muzzle");
	private static final Identifier COLLAR_ID = Identifier.of(MOD_ID, "collar");
	private static final Identifier LOCKED_COLLAR_ID = Identifier.of(MOD_ID, "locked_collar");
	private static final Identifier KEY_ID = Identifier.of(MOD_ID, "key");
	private static final Identifier MASTER_KEY_ID = Identifier.of(MOD_ID, "master_key");
	private static final Identifier SHOCK_CONTROLLER_ID = Identifier.of(MOD_ID, "shock_controller");
	private static final Identifier LOCKBOX_PHOTO_ID = Identifier.of(MOD_ID, "lockbox_photo");
	public static final Item LOCKBOX_PHOTO = Registry.register(
			Registries.ITEM,
			LOCKBOX_PHOTO_ID,
			new Item(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, LOCKBOX_PHOTO_ID))
					.maxCount(1))
	);
	public static final Item SHOCK_CONTROLLER = Registry.register(
			Registries.ITEM,
			SHOCK_CONTROLLER_ID,
			new ShockControllerItem(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, SHOCK_CONTROLLER_ID))
					.maxCount(1))
	);
	public static final Item MUZZLE = Registry.register(
			Registries.ITEM,
			MUZZLE_ID,
			new WearableItem(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, MUZZLE_ID))
					.maxCount(1), "head", "face")
	);
	public static final Item MUZZLE_RENDER = Registry.register(
			Registries.ITEM,
			MUZZLE_RENDER_ID,
			new Item(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, MUZZLE_RENDER_ID)).maxCount(1))
	);
	public static final Item LOCKED_MUZZLE = registerWearable(LOCKED_MUZZLE_ID, "head", "face");
	public static final Item COLLAR = registerWearable(COLLAR_ID, "chest", "necklace");
	public static final Item LOCKED_COLLAR = registerWearable(LOCKED_COLLAR_ID, "chest", "necklace");
	public static final Item KEY = Registry.register(
			Registries.ITEM,
			KEY_ID,
			new KeyItem(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, KEY_ID))
					.maxCount(1))
	);
	public static final Item MASTER_KEY = Registry.register(
			Registries.ITEM,
			MASTER_KEY_ID,
			new MasterKeyItem(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, MASTER_KEY_ID))
					.maxCount(1))
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
							entries.add(MASTER_KEY);
							entries.add(SHOCK_CONTROLLER);
							entries.add(LOCKBOX_PHOTO);
						})
						.build()
		);
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
			if (!isMuzzleEquipped(sender)) {
				return true;
			}

			String content = message.getContent().getString();
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
			Text muffledMessage = sender.getDisplayName().copy().append(Text.literal(": ")).append(Text.literal(muffledContent.toString()));
			sender.getEntityWorld().getServer().getPlayerManager().broadcast(
					muffledMessage,
					recipient -> muffledMessage,
					false
			);
			return false;
		});
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!(entity instanceof PlayerEntity target) || !player.getStackInHand(hand).isOf(Items.LEAD)) {
				return ActionResult.PASS;
			}
			if (world.isClient()) return ActionResult.PASS;
			return useLeadOnPlayer(player, target);
		});
		ServerTickEvents.END_SERVER_TICK.register(MuzzleGuardMod::tickLeashedPlayers);
		registerTrinketRules();
		registerAnimationBlocker();
	}

	private static ActionResult useLeadOnPlayer(PlayerEntity holder, PlayerEntity target) {
		if (holder == target) return ActionResult.SUCCESS;
		if (getEquippedCollar(target) == null) {
			holder.sendMessage(Text.translatable("message.muzzle_guard.leash_requires_collar"), false);
			return ActionResult.SUCCESS;
		}

		UUID currentHolder = getLeashHolderId(target);
		if (currentHolder != null) {
			if (currentHolder.equals(holder.getUuid())) {
				target.removeCommandTag(leashTag(currentHolder));
				holder.sendMessage(Text.translatable("message.muzzle_guard.leash_released", target.getDisplayName()), false);
				target.sendMessage(Text.translatable("message.muzzle_guard.leash_released_target", holder.getDisplayName()), false);
			} else {
				holder.sendMessage(Text.translatable("message.muzzle_guard.leash_already_attached"), false);
			}
			return ActionResult.SUCCESS;
		}

		if (getLeashHolderId(holder) != null) {
			holder.sendMessage(Text.translatable("message.muzzle_guard.leash_cannot_lead_while_leashed"), false);
			return ActionResult.SUCCESS;
		}
		for (ServerPlayerEntity online : holder.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
			if (!online.getUuid().equals(target.getUuid()) && holder.getUuid().equals(getLeashHolderId(online))) {
				holder.sendMessage(Text.translatable("message.muzzle_guard.leash_already_leading"), false);
				return ActionResult.SUCCESS;
			}
		}

		target.addCommandTag(leashTag(holder.getUuid()));
		holder.sendMessage(Text.translatable("message.muzzle_guard.leash_attached", target.getDisplayName()), false);
		target.sendMessage(Text.translatable("message.muzzle_guard.leash_attached_target", holder.getDisplayName()), false);
		return ActionResult.SUCCESS;
	}

	private static String leashTag(UUID holderId) {
		return LEASH_HOLDER_PREFIX + holderId;
	}

	private static UUID getLeashHolderId(PlayerEntity target) {
		for (String tag : target.getCommandTags()) {
			if (!tag.startsWith(LEASH_HOLDER_PREFIX)) continue;
			try {
				return UUID.fromString(tag.substring(LEASH_HOLDER_PREFIX.length()));
			} catch (IllegalArgumentException ignored) {
				target.removeCommandTag(tag);
			}
		}
		return null;
	}

	private static boolean isMuffledSyllable(int codePoint) {
		for (String syllable : MUFFLED_SYLLABLES) {
			if (syllable.codePointAt(0) == codePoint) return true;
		}
		return false;
	}

	private static void tickLeashedPlayers(MinecraftServer server) {
		for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
			UUID holderId = getLeashHolderId(target);
			if (holderId == null) continue;

			ServerPlayerEntity holder = server.getPlayerManager().getPlayer(holderId);
			if (holder == null) continue;
			if (target == holder || !target.getEntityWorld().getRegistryKey().equals(holder.getEntityWorld().getRegistryKey())
					) {
				target.removeCommandTag(leashTag(holderId));
				target.sendMessage(Text.translatable("message.muzzle_guard.leash_auto_released"), false);
				holder.sendMessage(Text.translatable("message.muzzle_guard.leash_auto_released_holder", target.getDisplayName()), false);
				continue;
			}

			double distance = Math.sqrt(target.squaredDistanceTo(holder));
			Vec3d targetAnchor = new Vec3d(target.getX(), target.getY() + 1.1, target.getZ());
			Vec3d holderAnchor = new Vec3d(holder.getX(), holder.getY() + 1.1, holder.getZ());
			Vec3d tether = holderAnchor.subtract(targetAnchor);
			int segments = Math.max(1, (int) (distance * 3.0));
			ServerWorld world = (ServerWorld) target.getEntityWorld();
			for (int segment = 0; segment <= segments; segment++) {
				Vec3d point = targetAnchor.add(tether.multiply((double) segment / segments));
				world.spawnParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
			}
			if (distance > 2.5) {
				Vec3d pull = tether.normalize();
				double strength = Math.min(0.35, (distance - 2.0) * 0.08);
				target.addVelocity(pull.multiply(strength));
			}
		}
	}

	private static boolean removeLockBinding(PlayerEntity actor, PlayerEntity target) {
		try {
			Map<?, ?> groups = getTrinketInventories(target);
			if (groups == null) {
				actor.sendMessage(Text.translatable("message.muzzle_guard.key_no_trinkets"), false);
				return false;
			}
			Class<?> inventoryApi = Class.forName("dev.emi.trinkets.api.TrinketInventory");
			for (String[] slotPath : new String[][]{{"head", "face"}, {"chest", "necklace"}}) {
				Object slots = groups.get(slotPath[0]);
				Object inventory = slots instanceof Map<?, ?> slotMap ? slotMap.get(slotPath[1]) : null;
				if (inventory == null) continue;
				int size = ((Number) invokeApiMethod(inventoryApi, inventory,
						new String[]{"size", "method_5439"}, new Class<?>[0])).intValue();
				for (int index = 0; index < size; index++) {
					ItemStack worn = (ItemStack) invokeApiMethod(inventoryApi, inventory,
							new String[]{"getStack", "method_5438"}, new Class<?>[]{int.class}, index);
					if (worn.isEmpty() || !(worn.isOf(LOCKED_COLLAR) || worn.isOf(LOCKED_MUZZLE))) continue;
					NbtCompound data = customData(worn);
					data.remove(BOUND_KEY_UUID);
					data.remove(LOCKED);
					setCustomData(worn, data);
					inventoryApi.getMethod("markUpdate").invoke(inventory);
					Text result = Text.translatable("message.muzzle_guard.master_key_unlocked");
					actor.sendMessage(result, false);
					if (!actor.getUuid().equals(target.getUuid())) target.sendMessage(result, false);
					return true;
				}
			}
			actor.sendMessage(Text.translatable("message.muzzle_guard.key_no_target"), false);
			return false;
		} catch (ReflectiveOperationException exception) {
			System.err.println("[Muzzle Guard] Failed to remove Trinket lock binding:");
			exception.printStackTrace(System.err);
			actor.sendMessage(Text.translatable("message.muzzle_guard.key_error"), false);
			return false;
		}
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
		public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent display, Consumer<Text> textConsumer, TooltipType type) {
			super.appendTooltip(stack, context, display, textConsumer, type);
			if (this != LOCKED_MUZZLE && this != LOCKED_COLLAR) return;

			NbtCompound data = customData(stack);
			String boundKey = data.getString(BOUND_KEY_UUID).orElse("");
			textConsumer.accept(Text.translatable(boundKey.isEmpty()
					? "tooltip.muzzle_guard.unbound"
					: "tooltip.muzzle_guard.bound_key", shortId(boundKey)));
			if (!boundKey.isEmpty()) {
				textConsumer.accept(Text.translatable(data.getBoolean(LOCKED).orElse(false)
						? "tooltip.muzzle_guard.locked"
						: "tooltip.muzzle_guard.unlocked"));
			}
		}

		@Override
		public ActionResult use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!world.isClient()) {
				if (equipHeldItem(user, user, stack, group, slot)) {
					user.sendMessage(Text.translatable("message.muzzle_guard.equipped_self"), false);
				} else {
					user.sendMessage(Text.translatable("message.muzzle_guard.equip_failed"), false);
				}
			}
			return ActionResult.SUCCESS;
		}

		@Override
		public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
			if (!user.getEntityWorld().isClient() && entity instanceof PlayerEntity target) {
				ItemStack held = user.getStackInHand(hand);
				if (equipHeldItem(user, target, held, group, slot)) {
					Text message = Text.translatable("message.muzzle_guard.equipped_other", target.getDisplayName());
					user.sendMessage(message, false);
					if (!user.getUuid().equals(target.getUuid())) {
						target.sendMessage(Text.translatable("message.muzzle_guard.equipped_by", user.getDisplayName()), false);
					}
				} else {
					user.sendMessage(Text.translatable("message.muzzle_guard.equip_failed"), false);
				}
			}
			return ActionResult.SUCCESS;
		}
	}

	private static final class KeyItem extends Item {
		private KeyItem(Settings settings) {
			super(settings);
		}

		@Override
		public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent display, Consumer<Text> textConsumer, TooltipType type) {
			super.appendTooltip(stack, context, display, textConsumer, type);
			String id = customData(stack).getString(KEY_UUID).orElse("");
			if (!id.isEmpty()) textConsumer.accept(Text.translatable("tooltip.muzzle_guard.key_id", shortId(id)));
		}

		@Override
		public ActionResult use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!world.isClient()) bindOrToggle(stack, user, user);
			return ActionResult.SUCCESS;
		}

		@Override
		public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
			if (!user.getEntityWorld().isClient() && entity instanceof PlayerEntity target) {
				bindOrToggle(user.getStackInHand(hand), user, target);
			}
			return ActionResult.SUCCESS;
		}
	}

	private static final class MasterKeyItem extends Item {
		private MasterKeyItem(Settings settings) {
			super(settings);
		}

		@Override
		public ActionResult use(World world, PlayerEntity user, Hand hand) {
			if (!world.isClient() && removeLockBinding(user, user)) consumeIfSurvival(user, hand);
			return ActionResult.SUCCESS;
		}

		@Override
		public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
			if (!user.getEntityWorld().isClient() && entity instanceof PlayerEntity target
					&& removeLockBinding(user, target)) {
				consumeIfSurvival(user, hand);
			}
			return ActionResult.SUCCESS;
		}

		private static void consumeIfSurvival(PlayerEntity user, Hand hand) {
			if (!user.getAbilities().creativeMode) user.getStackInHand(hand).decrement(1);
		}
	}

	private static final class ShockControllerItem extends Item {
		private ShockControllerItem(Settings settings) {
			super(settings);
		}

		@Override
		public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent display, Consumer<Text> textConsumer, TooltipType type) {
			super.appendTooltip(stack, context, display, textConsumer, type);
			NbtCompound data = customData(stack);
			String playerId = data.getString(CONTROLLER_PLAYER_UUID).orElse("");
			String collarId = data.getString(CONTROLLER_COLLAR_UUID).orElse("");
			if (!playerId.isEmpty() && !collarId.isEmpty()) {
				textConsumer.accept(Text.translatable("tooltip.muzzle_guard.controller_bound", shortId(playerId), shortId(collarId)));
			}
		}

		@Override
		public ActionResult use(World world, PlayerEntity user, Hand hand) {
			if (!world.isClient()) useController(user.getStackInHand(hand), user, user);
			return ActionResult.SUCCESS;
		}

		@Override
		public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
			if (!user.getEntityWorld().isClient() && entity instanceof PlayerEntity target) {
				useController(user.getStackInHand(hand), user, target);
			}
			return ActionResult.SUCCESS;
		}
	}

	private static void useController(ItemStack controller, PlayerEntity user, PlayerEntity selectedPlayer) {
		NbtCompound controllerData = customData(controller);
		String playerId = controllerData.getString(CONTROLLER_PLAYER_UUID).orElse("");
		String collarId = controllerData.getString(CONTROLLER_COLLAR_UUID).orElse("");
		if (playerId.isEmpty() || collarId.isEmpty()) {
			ItemStack collar = getEquippedCollar(selectedPlayer);
			if (collar == null) {
				user.sendMessage(Text.translatable("message.muzzle_guard.controller_no_collar"), false);
				return;
			}
			if (collar.isOf(LOCKED_COLLAR) && customData(collar).getBoolean(LOCKED).orElse(false)
					&& !hasMatchingKey(user, customData(collar).getString(BOUND_KEY_UUID).orElse(""))) {
				user.sendMessage(Text.translatable("message.muzzle_guard.controller_collar_locked"), false);
				return;
			}
			collarId = getOrCreateCollarId(collar);
			markNecklaceInventoryUpdated(selectedPlayer);
			controllerData.putString(CONTROLLER_PLAYER_UUID, selectedPlayer.getUuidAsString());
			controllerData.putString(CONTROLLER_COLLAR_UUID, collarId);
			setCustomData(controller, controllerData);
			user.sendMessage(Text.translatable("message.muzzle_guard.controller_bound", selectedPlayer.getDisplayName(), shortId(collarId)), false);
			return;
		}

		PlayerEntity target;
		try {
			target = user.getEntityWorld().getServer().getPlayerManager().getPlayer(UUID.fromString(playerId));
		} catch (IllegalArgumentException exception) {
			target = null;
		}
		if (target == null || !collarId.equals(getEquippedCollarId(target))) {
			controllerData.remove(CONTROLLER_PLAYER_UUID);
			controllerData.remove(CONTROLLER_COLLAR_UUID);
			setCustomData(controller, controllerData);
			user.sendMessage(Text.translatable("message.muzzle_guard.controller_unbound"), false);
			return;
		}

		target.damage((ServerWorld) target.getEntityWorld(), user.getDamageSources().magic(), 1.0F);
		user.sendMessage(Text.translatable("message.muzzle_guard.controller_shocked", target.getDisplayName()), false);
		if (!user.getUuid().equals(target.getUuid())) {
			target.sendMessage(Text.translatable("message.muzzle_guard.controller_shocked_target"), false);
		}
	}

	private static boolean hasMatchingKey(PlayerEntity player, String boundKeyId) {
		if (boundKeyId.isEmpty()) return false;
		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			ItemStack stack = player.getInventory().getStack(slot);
			if (stack.isOf(KEY) && boundKeyId.equals(customData(stack).getString(KEY_UUID).orElse(""))) return true;
		}
		for (Hand hand : Hand.values()) {
			ItemStack stack = player.getStackInHand(hand);
			if (stack.isOf(KEY) && boundKeyId.equals(customData(stack).getString(KEY_UUID).orElse(""))) return true;
		}
		return false;
	}

	private static ItemStack getEquippedCollar(PlayerEntity player) {
		try {
			Map<?, ?> groups = getTrinketInventories(player);
			Object slots = groups == null ? null : groups.get("chest");
			Object inventory = slots instanceof Map<?, ?> slotMap ? slotMap.get("necklace") : null;
			if (inventory == null) return null;
			Class<?> inventoryApi = Class.forName("dev.emi.trinkets.api.TrinketInventory");
			int size = ((Number) invokeApiMethod(inventoryApi, inventory,
					new String[]{"size", "method_5439"}, new Class<?>[0])).intValue();
			for (int index = 0; index < size; index++) {
				ItemStack stack = (ItemStack) invokeApiMethod(inventoryApi, inventory,
						new String[]{"getStack", "method_5438"}, new Class<?>[]{int.class}, index);
				if (stack.isOf(COLLAR) || stack.isOf(LOCKED_COLLAR)) return stack;
			}
		} catch (ReflectiveOperationException exception) {
			System.err.println("[Muzzle Guard] Failed to inspect the player's collar: " + exception);
		}
		return null;
	}

	private static String getEquippedCollarId(PlayerEntity player) {
		ItemStack collar = getEquippedCollar(player);
		return collar == null ? "" : customData(collar).getString(COLLAR_UUID).orElse("");
	}

	private static String getOrCreateCollarId(ItemStack collar) {
		NbtCompound data = customData(collar);
		String id = data.getString(COLLAR_UUID).orElse("");
		if (id.isEmpty()) {
			id = UUID.randomUUID().toString();
			data.putString(COLLAR_UUID, id);
			setCustomData(collar, data);
		}
		return id;
	}

	private static void markNecklaceInventoryUpdated(PlayerEntity player) {
		try {
			Map<?, ?> groups = getTrinketInventories(player);
			Object slots = groups == null ? null : groups.get("chest");
			Object inventory = slots instanceof Map<?, ?> slotMap ? slotMap.get("necklace") : null;
			if (inventory != null) Class.forName("dev.emi.trinkets.api.TrinketInventory").getMethod("markUpdate").invoke(inventory);
		} catch (ReflectiveOperationException exception) {
			System.err.println("[Muzzle Guard] Failed to sync collar identity: " + exception);
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
				if (!player.getAbilities().creativeMode) {
					held.decrement(1);
					player.getInventory().markDirty();
				}
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

	private static boolean bindOrToggle(ItemStack key, PlayerEntity actor, PlayerEntity target) {
		try {
			Map<?, ?> groups = getTrinketInventories(target);
			if (groups == null) {
				actor.sendMessage(Text.translatable("message.muzzle_guard.key_no_trinkets"), false);
				return false;
			}
			Class<?> inventoryApi = Class.forName("dev.emi.trinkets.api.TrinketInventory");
			String id = customData(key).getString(KEY_UUID).orElse("");
			String boundKeyId = id;
			List<LockCandidate> candidates = new ArrayList<>();
			for (String[] slotPath : new String[][]{{"head", "face"}, {"chest", "necklace"}}) {
				Object slots = groups.get(slotPath[0]);
				Object inventory = slots instanceof Map<?, ?> slotMap ? slotMap.get(slotPath[1]) : null;
				if (inventory == null) continue;
				int size = ((Number) invokeApiMethod(inventoryApi, inventory,
						new String[]{"size", "method_5439"}, new Class<?>[0])).intValue();
				for (int index = 0; index < size; index++) {
					ItemStack worn = (ItemStack) invokeApiMethod(inventoryApi, inventory,
							new String[]{"getStack", "method_5438"}, new Class<?>[]{int.class}, index);
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
					actor.sendMessage(Text.translatable("message.muzzle_guard.key_no_target"), false);
				} else {
					actor.sendMessage(Text.translatable("message.muzzle_guard.key_bound_elsewhere", candidates.get(0).stack.getName()), false);
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
			inventoryApi.getMethod("markUpdate").invoke(selected.inventory);
			String message = newlyBound
					? "message.muzzle_guard.key_bound_to"
					: selected.data.getBoolean(LOCKED).orElse(false)
							? "message.muzzle_guard.locked_item"
							: "message.muzzle_guard.unlocked_item";
			Text result = Text.translatable(message, selected.stack.getName());
			actor.sendMessage(result, false);
			if (!actor.getUuid().equals(target.getUuid())) target.sendMessage(result, false);
			return true;
		} catch (ReflectiveOperationException exception) {
			System.err.println("[Muzzle Guard] Failed to bind or toggle Trinket lock:");
			exception.printStackTrace(System.err);
			actor.sendMessage(Text.translatable("message.muzzle_guard.key_error"), false);
		}
		return false;
	}

	private static boolean isLockableTrinket(ItemStack stack) {
		return !stack.isEmpty() && (stack.isOf(LOCKED_MUZZLE) || stack.isOf(LOCKED_COLLAR));
	}

	private static final class LockCandidate {
		private final ItemStack stack;
		private final Object inventory;
		private final NbtCompound data;
		private final String boundKey;

		private LockCandidate(ItemStack stack, Object inventory) {
			this.stack = stack;
			this.inventory = inventory;
			this.data = customData(stack);
			this.boundKey = data.getString(BOUND_KEY_UUID).orElse("");
		}
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

	private static String shortId(String id) {
		return id.length() <= 8 ? id : id.substring(0, 8);
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
						ItemStack stack = args != null && args.length > 0 && args[0] instanceof ItemStack itemStack
								? itemStack
								: ItemStack.EMPTY;
						boolean locked = lockable && !stack.isEmpty()
								&& customData(stack).getBoolean(LOCKED).orElse(false);
						boolean canUnequip = !lockable || (!stack.isEmpty() && !locked);
						if (item == COLLAR || item == LOCKED_COLLAR) {
							String side = args != null && args.length > 2 && args[2] instanceof LivingEntity entity
									&& entity.getEntityWorld().isClient() ? "client" : "server";
							System.out.println("[Muzzle Guard] Necklace canUnequip (" + side + "): item="
									+ Registries.ITEM.getId(stack.getItem()) + ", locked=" + locked
									+ ", allowed=" + canUnequip);
						}
						return canUnequip;
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
			return false;
		} catch (ReflectiveOperationException | ClassCastException exception) {
			System.err.println("[Muzzle Guard] Failed to inspect a Needs of Nature animation: " + exception);
			return false;
		}
	}
}
