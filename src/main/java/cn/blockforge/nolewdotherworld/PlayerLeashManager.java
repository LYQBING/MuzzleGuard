package cn.blockforge.nolewdotherworld;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

public final class PlayerLeashManager {
	private static final double BREAK_DISTANCE = 10.0;
	private static final double PULL_START_DISTANCE = 3.0;
	private static final double ANCHOR_HEIGHT = 1.125;
	private static final String ANCHOR_MARKER = "no_lewd_otherworld:player_leash_anchor";
	private static final Map<UUID, Link> LEASHES_BY_TARGET = new HashMap<>();

	private PlayerLeashManager() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register(PlayerLeashManager::onUseEntity);
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
			isLeashAnchor(entity) ? InteractionResult.FAIL : InteractionResult.PASS);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			!isLeashAnchor(entity));
		ServerTickEvents.END_SERVER_TICK.register(PlayerLeashManager::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			removeLinksFor(handler.player));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> clearLinks());
	}

	private static InteractionResult onUseEntity(Player player, Level world, InteractionHand hand,
			Entity entity, net.minecraft.world.phys.EntityHitResult hitResult) {
		if (isLeashAnchor(entity)) return InteractionResult.SUCCESS;
		if (isLeashFenceKnot(entity) && hasLeashAnchorAttachedTo(entity, world)) {
			return InteractionResult.SUCCESS;
		}
		if (!(entity instanceof Player)) return InteractionResult.PASS;
		boolean holdingLead = player.getItemInHand(hand).is(Items.LEAD);
		if (world.isClientSide()) return holdingLead ? InteractionResult.SUCCESS : InteractionResult.PASS;
		if (!(player instanceof ServerPlayer holder) || !(entity instanceof ServerPlayer target)) {
			return InteractionResult.FAIL;
		}

		Link existing = LEASHES_BY_TARGET.get(target.getUUID());
		boolean takingOverFenceLink = false;
		if (existing != null) {
			if (existing.holderId.equals(holder.getUUID())) {
				removeLink(target.getUUID(), existing);
				return InteractionResult.SUCCESS;
			}
			if (!holdingLead || !isLeashFenceKnot(existing.anchor.getLeashHolder())) {
				return InteractionResult.FAIL;
			}
			takingOverFenceLink = true;
		}
		if (!holdingLead) return InteractionResult.PASS;
		if (wouldCreateCycle(holder.getUUID(), target.getUUID())) return InteractionResult.FAIL;
		if (!NoLewdOtherworldMod.canLeashPlayer(holder, target)) return InteractionResult.FAIL;

		if (!(world instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
		EntityType<?> allayType = BuiltInRegistries.ENTITY_TYPE.get(
				Identifier.fromNamespaceAndPath("minecraft", "allay"))
				.map(Holder.Reference::value).orElse(null);
		if (allayType == null) return InteractionResult.PASS;
		if (!(allayType.create(serverLevel, EntitySpawnReason.COMMAND) instanceof Mob anchor)) return InteractionResult.PASS;
		anchor.setNoAi(true);
		anchor.setNoPhysics(true);
		anchor.setNoGravity(true);
		anchor.setInvulnerable(true);
		anchor.setSilent(true);
		anchor.setInvisible(true);
		anchor.setCustomName(Component.literal(ANCHOR_MARKER));
		anchor.setCustomNameVisible(false);
		anchor.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, true, false, false));
		anchor.setPos(target.getX(), target.getY() + ANCHOR_HEIGHT, target.getZ());
		if (!serverLevel.addFreshEntity(anchor)) return InteractionResult.PASS;
		anchor.setLeashedTo(holder, true);
		if (takingOverFenceLink) cleanupLink(existing);
		boolean leadConsumed = !holder.getAbilities().instabuild;
		if (leadConsumed) holder.getItemInHand(hand).shrink(1);
		LEASHES_BY_TARGET.put(target.getUUID(), new Link(holder, anchor, leadConsumed));
		return InteractionResult.SUCCESS;
	}

	public static boolean isLeashAnchor(Entity entity) {
		return entity.getCustomName() != null && ANCHOR_MARKER.equals(entity.getCustomName().getString());
	}

	private static boolean wouldCreateCycle(UUID holderId, UUID targetId) {
		Set<UUID> visited = new HashSet<>();
		UUID currentId = holderId;
		while (visited.add(currentId)) {
			if (currentId.equals(targetId)) return true;
			Link link = LEASHES_BY_TARGET.get(currentId);
			if (link == null) return false;
			currentId = link.holderId;
		}
		return true;
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Link>> links = LEASHES_BY_TARGET.entrySet().iterator();
		while (links.hasNext()) {
			Map.Entry<UUID, Link> entry = links.next();
			ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
			ServerPlayer holder = server.getPlayerList().getPlayer(entry.getValue().holderId);
			Link link = entry.getValue();
			if (target == null || holder == null || target == holder || target.isDeadOrDying()
					|| holder.isDeadOrDying() || target.level() != holder.level()
					|| !NoLewdOtherworldMod.isCollarEquipped(target) || link.anchor.isRemoved()) {
				cleanupLink(link);
				links.remove();
				continue;
			}

			Entity leashHolder = link.anchor.getLeashHolder();
			if (leashHolder != holder) {
				if (leashHolder != null && isLeashFenceKnot(leashHolder)) {
					link.anchor.setPos(target.getX(), target.getY() + ANCHOR_HEIGHT, target.getZ());
					continue;
				}
				cleanupLink(link);
				links.remove();
				continue;
			}

			double dx = holder.getX() - target.getX();
			double dy = holder.getY() + 0.8 - (target.getY() + 1.0);
			double dz = holder.getZ() - target.getZ();
			double distanceSquared = dx * dx + dy * dy + dz * dz;
			if (distanceSquared > BREAK_DISTANCE * BREAK_DISTANCE) {
				cleanupLink(link);
				links.remove();
				continue;
			}

			link.anchor.setPos(target.getX(), target.getY() + ANCHOR_HEIGHT, target.getZ());
			double distance = Math.sqrt(distanceSquared);
			var velocity = target.getDeltaMovement();
			if (distance > PULL_START_DISTANCE) {
				double strength = Math.min(0.12, (distance - PULL_START_DISTANCE) * 0.04);
				target.setDeltaMovement(velocity.add(
						dx / distance * strength, dy / distance * strength, dz / distance * strength));
				target.connection.send(new ClientboundSetEntityMotionPacket(target));
			} else {
				double holderSpeedSquared = holder.getDeltaMovement().horizontalDistanceSqr();
				if (holderSpeedSquared < 0.0004) {
					target.setDeltaMovement(velocity.x * 0.6, velocity.y, velocity.z * 0.6);
					target.connection.send(new ClientboundSetEntityMotionPacket(target));
				}
			}
		}
	}

	private static boolean isLeashFenceKnot(Entity entity) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
				.equals(Identifier.fromNamespaceAndPath("minecraft", "leash_knot"));
	}

	private static boolean hasLeashAnchorAttachedTo(Entity leashKnot, Level world) {
		return !world.getEntitiesOfClass(Mob.class, leashKnot.getBoundingBox().inflate(8.0),
				anchor -> isLeashAnchor(anchor) && anchor.getLeashHolder() == leashKnot).isEmpty();
	}

	private static void removeLinksFor(ServerPlayer disconnectingPlayer) {
		UUID playerId = disconnectingPlayer.getUUID();
		Iterator<Map.Entry<UUID, Link>> links = LEASHES_BY_TARGET.entrySet().iterator();
		while (links.hasNext()) {
			Map.Entry<UUID, Link> entry = links.next();
			if (entry.getKey().equals(playerId) || entry.getValue().holderId.equals(playerId)) {
				cleanupLink(entry.getValue());
				links.remove();
			}
		}
	}

	private static void removeLink(UUID targetId, Link link) {
		LEASHES_BY_TARGET.remove(targetId);
		cleanupLink(link);
	}

	private static void cleanupLink(Link link) {
		link.anchor.discard();
		returnLead(link.holder, link);
	}

	private static void clearLinks() {
		for (Link link : LEASHES_BY_TARGET.values()) {
			cleanupLink(link);
		}
		LEASHES_BY_TARGET.clear();
	}

	private static void returnLead(ServerPlayer holder, Link link) {
		if (!link.leadConsumed) return;
		ItemStack lead = new ItemStack(Items.LEAD);
		if (!holder.getInventory().add(lead)) {
			holder.level().addFreshEntity(new ItemEntity(
					holder.level(), holder.getX(), holder.getY(), holder.getZ(), lead));
		}
		link.leadConsumed = false;
	}

	private static final class Link {
		private final UUID holderId;
		private final ServerPlayer holder;
		private final Mob anchor;
		private boolean leadConsumed;

		private Link(ServerPlayer holder, Mob anchor, boolean leadConsumed) {
			this.holderId = holder.getUUID();
			this.holder = holder;
			this.anchor = anchor;
			this.leadConsumed = leadConsumed;
		}
	}
}
