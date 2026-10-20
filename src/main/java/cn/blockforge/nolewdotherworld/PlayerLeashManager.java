package cn.blockforge.nolewdotherworld;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class PlayerLeashManager {
	private static final double BREAK_DISTANCE = 10.0;
	private static final double PULL_START_DISTANCE = 3.0;
	private static final double PULL_RELEASE_DISTANCE = 2.5;
	private static final double ANCHOR_HEIGHT = 1.125;
	private static final Map<UUID, Link> LEASHES_BY_TARGET = new HashMap<>();

	private PlayerLeashManager() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register(PlayerLeashManager::onUseEntity);
		ServerTickEvents.END_SERVER_TICK.register(PlayerLeashManager::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			removeLinksFor(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> clearLinks());
	}

	private static InteractionResult onUseEntity(Player player, Level world, InteractionHand hand,
			net.minecraft.world.entity.Entity entity, net.minecraft.world.phys.EntityHitResult hitResult) {
		if (world.isClientSide() || !(player instanceof ServerPlayer holder)
				|| !(entity instanceof ServerPlayer target) || holder == target
				|| !holder.getItemInHand(hand).is(Items.LEAD)) {
			return InteractionResult.PASS;
		}

		Link existing = LEASHES_BY_TARGET.get(target.getUUID());
		if (existing != null) {
			if (!existing.holderId.equals(holder.getUUID())) return InteractionResult.FAIL;
			removeLink(target.getUUID(), existing);
			return InteractionResult.SUCCESS;
		}
		if (!NoLewdOtherworldMod.isCollarEquipped(target)) return InteractionResult.PASS;

		if (!(world instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
		EntityType<?> allayType = BuiltInRegistries.ENTITY_TYPE.get(
				Identifier.fromNamespaceAndPath("minecraft", "allay"))
				.map(Holder.Reference::value).orElse(null);
		if (allayType == null) return InteractionResult.PASS;
		if (!(allayType.create(serverLevel, EntitySpawnReason.COMMAND) instanceof Mob anchor)) return InteractionResult.PASS;
		anchor.setNoAi(true);
		anchor.setNoGravity(true);
		anchor.setSilent(true);
		anchor.setInvisible(true);
		anchor.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, true, false, false));
		anchor.setPos(target.getX(), target.getY() + ANCHOR_HEIGHT, target.getZ());
		if (!serverLevel.addFreshEntity(anchor)) return InteractionResult.PASS;
		anchor.setLeashedTo(holder, true);
		LEASHES_BY_TARGET.put(target.getUUID(), new Link(holder.getUUID(), anchor));
		return InteractionResult.SUCCESS;
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
				link.anchor.discard();
				links.remove();
				continue;
			}

			double dx = holder.getX() - target.getX();
			double dy = holder.getY() + 0.8 - (target.getY() + 1.0);
			double dz = holder.getZ() - target.getZ();
			double distanceSquared = dx * dx + dy * dy + dz * dz;
			if (distanceSquared > BREAK_DISTANCE * BREAK_DISTANCE) {
				link.anchor.discard();
				links.remove();
				continue;
			}

			link.anchor.setPos(target.getX(), target.getY() + ANCHOR_HEIGHT, target.getZ());
			double distance = Math.sqrt(distanceSquared);
			var velocity = target.getDeltaMovement();
			if (link.pulling ? distance > PULL_RELEASE_DISTANCE : distance > PULL_START_DISTANCE) {
				link.pulling = true;
				double strength = Math.min(0.12, (distance - PULL_START_DISTANCE) * 0.04);
				target.setDeltaMovement(velocity.add(
						dx / distance * strength, dy / distance * strength, dz / distance * strength));
				target.connection.send(new ClientboundSetEntityMotionPacket(target));
			} else {
				link.pulling = false;
				double holderSpeedSquared = holder.getDeltaMovement().horizontalDistanceSqr();
				if (holderSpeedSquared < 0.0004 && distance < PULL_START_DISTANCE) {
					target.setDeltaMovement(velocity.x * 0.6, velocity.y, velocity.z * 0.6);
					target.connection.send(new ClientboundSetEntityMotionPacket(target));
				}
			}
		}
	}

	private static void removeLinksFor(UUID playerId) {
		Iterator<Map.Entry<UUID, Link>> links = LEASHES_BY_TARGET.entrySet().iterator();
		while (links.hasNext()) {
			Map.Entry<UUID, Link> entry = links.next();
			if (entry.getKey().equals(playerId) || entry.getValue().holderId.equals(playerId)) {
				entry.getValue().anchor.discard();
				links.remove();
			}
		}
	}

	private static void removeLink(UUID targetId, Link link) {
		LEASHES_BY_TARGET.remove(targetId);
		link.anchor.discard();
	}

	private static void clearLinks() {
		for (Link link : LEASHES_BY_TARGET.values()) link.anchor.discard();
		LEASHES_BY_TARGET.clear();
	}

	private static final class Link {
		private final UUID holderId;
		private final Mob anchor;
		private boolean pulling;

		private Link(UUID holderId, Mob anchor) {
			this.holderId = holderId;
			this.anchor = anchor;
		}
	}
}
