package cn.blockforge.nolewdotherworld;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import net.fabricmc.api.ModInitializer;
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
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.component.TrinketDataComponents;
import eu.pb4.trinkets.api.component.TrinketEquippable;

public final class NoLewdOtherworldMod implements ModInitializer {
	public static final String MOD_ID = "no_lewd_otherworld";
	private static final List<Item> FACE_WEARABLES = new ArrayList<>();
	private static final List<Item> NECKLACE_WEARABLES = new ArrayList<>();
	private static final Identifier BALL_GAG_ID = Identifier.fromNamespaceAndPath(MOD_ID, "ball_gag");
	private static final Identifier LOCKED_BALL_GAG_ID = Identifier.fromNamespaceAndPath(MOD_ID, "locked_ball_gag");
	private static final Identifier BIT_GAG_ID = Identifier.fromNamespaceAndPath(MOD_ID, "bit_gag");
	private static final Identifier LOCKED_BIT_GAG_ID = Identifier.fromNamespaceAndPath(MOD_ID, "locked_bit_gag");
	private static final Identifier RING_GAG_ID = Identifier.fromNamespaceAndPath(MOD_ID, "ring_gag");
	private static final Identifier LOCKED_RING_GAG_ID = Identifier.fromNamespaceAndPath(MOD_ID, "locked_ring_gag");
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
	public static final Item BALL_GAG = registerWearable(BALL_GAG_ID, "head", "face");
	public static final Item LOCKED_BALL_GAG = registerWearable(LOCKED_BALL_GAG_ID, "head", "face");
	public static final Item BIT_GAG = registerWearable(BIT_GAG_ID, "head", "face");
	public static final Item LOCKED_BIT_GAG = registerWearable(LOCKED_BIT_GAG_ID, "head", "face");
	public static final Item RING_GAG = registerWearable(RING_GAG_ID, "head", "face");
	public static final Item LOCKED_RING_GAG = registerWearable(LOCKED_RING_GAG_ID, "head", "face");
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
		Item item = Registry.register(
				BuiltInRegistries.ITEM,
				id,
				new WearableItem(new Item.Properties()
						.setId(ResourceKey.create(Registries.ITEM, id))
						.component(TrinketDataComponents.EQUIPMENT,
								TrinketEquippable.DEFAULT.withSlots(group + "/" + slot))
						.stacksTo(1), group, slot)
		);
		if ("head".equals(group) && "face".equals(slot)) FACE_WEARABLES.add(item);
		if ("chest".equals(group) && "necklace".equals(slot)) NECKLACE_WEARABLES.add(item);
		return item;
	}

	public static List<Item> faceWearables() {
		return List.copyOf(FACE_WEARABLES);
	}

	public static List<Item> necklaceWearables() {
		return List.copyOf(NECKLACE_WEARABLES);
	}

	@Override
	public void onInitialize() {
		Registry.register(
				BuiltInRegistries.CREATIVE_MODE_TAB,
				Identifier.fromNamespaceAndPath(MOD_ID, "main"),
				CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
				.title(Component.translatable("itemGroup.no_lewd_otherworld"))
						.icon(() -> new ItemStack(BALL_GAG))
						.displayItems((context, entries) -> {
							faceWearables().forEach(entries::accept);
							necklaceWearables().forEach(entries::accept);
							entries.accept(KEY);
							entries.accept(MASTER_KEY);
							entries.accept(SHOCK_CONTROLLER);
							entries.accept(LOCKBOX_PHOTO);
						})
						.build()
		);
		GagChatHandler.register();
		LockService.register();
		PlayerAccessManager.register();
		PlayerLeashManager.register();
		registerAnimationBlocker();
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
			LockService.appendWearableTooltip(stack, textConsumer);
		}

		@Override
		public InteractionResult use(Level level, Player user, InteractionHand hand) {
			ItemStack stack = user.getItemInHand(hand);
			if (!level.isClientSide()) {
				if (equipHeldItem(user, user, stack, slot)) {
					user.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.equipped_self"));
				} else {
					user.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.equip_failed"));
				}
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
			if (!user.level().isClientSide() && entity instanceof Player target) {
				ItemStack held = user.getItemInHand(hand);
				if (equipHeldItem(user, target, held, slot)) {
					Component message = Component.translatable("message.no_lewd_otherworld.equipped_other", target.getDisplayName());
					user.sendSystemMessage(message);
					if (!user.getUUID().equals(target.getUUID())) {
						target.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.equipped_by", user.getDisplayName()));
					}
				} else {
					user.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.equip_failed"));
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
			LockService.appendKeyTooltip(stack, textConsumer);
		}

		@Override
		public InteractionResult use(Level level, Player user, InteractionHand hand) {
			ItemStack stack = user.getItemInHand(hand);
			if (!level.isClientSide()) LockService.bindOrToggle(stack, user, user);
			return InteractionResult.SUCCESS;
		}

		@Override
		public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
			if (!user.level().isClientSide() && entity instanceof Player target) {
				LockService.bindOrToggle(user.getItemInHand(hand), user, target);
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
			if (!level.isClientSide() && LockService.removeBinding(user, user)) consumeIfSurvival(user, hand);
			return InteractionResult.SUCCESS;
		}

		@Override
		public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
			if (!user.level().isClientSide() && entity instanceof Player target
					&& LockService.removeBinding(user, target)) {
				consumeIfSurvival(user, hand);
			}
			return InteractionResult.SUCCESS;
		}

		private static void consumeIfSurvival(Player user, InteractionHand hand) {
			if (!user.getAbilities().instabuild) user.getItemInHand(hand).shrink(1);
		}
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

	static String shortId(String id) {
		return id.length() <= 8 ? id : id.substring(0, 8);
	}

	static CompoundTag customData(ItemStack stack) {
		CustomData component = stack.get(DataComponents.CUSTOM_DATA);
		return component == null ? new CompoundTag() : component.copyTag();
	}

	static void setCustomData(ItemStack stack, CompoundTag data) {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
	}

	private static boolean isProtectedByKeepsake(Player player) {
		return !PlayerAccessManager.isAuthorized(player.getUUID())
				|| TrinketsApi.getAttachment(player).isEquipped(LOCKBOX_PHOTO);
	}

	private static void registerAnimationBlocker() {
		try {
			ClassLoader loader = NoLewdOtherworldMod.class.getClassLoader();
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
					NoLewdOtherworldMod::invokeCallback
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
				case "toString" -> "NoLewdOtherworldAllowStartListener";
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
			for (UUID actorId : actors) {
				Player player = world.getPlayerByUUID(actorId);
				if (player != null && isProtectedByKeepsake(player)) {
					protectedPlayers.add(player);
				}
			}
			if (requester != null && isProtectedByKeepsake(requester) && !protectedPlayers.contains(requester)) {
				protectedPlayers.add(requester);
			}
			return protectedPlayers.isEmpty();
		} catch (ReflectiveOperationException | ClassCastException exception) {
			System.err.println("[No Lewd Otherworld] Failed to inspect a Needs of Nature animation: " + exception);
			return false;
		}
	}
}
