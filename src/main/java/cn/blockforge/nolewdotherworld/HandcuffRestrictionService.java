package cn.blockforge.nolewdotherworld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public final class HandcuffRestrictionService {
	private static final long MESSAGE_INTERVAL_MS = 1000;
	private static final Map<UUID, Long> LAST_MESSAGE_TIMES = new HashMap<>();
	private static final String MESSAGE_KEY = "message.no_lewd_otherworld.handcuffs_restricted";
	private static Consumer<Player> clientFeedback = player -> { };

	private HandcuffRestrictionService() {
	}

	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				LAST_MESSAGE_TIMES.remove(handler.player.getUUID()));
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
				denyIfHandcuffed(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
				denyIfHandcuffed(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
				!denyIfHandcuffed(player));
		UseItemCallback.EVENT.register((player, world, hand) ->
				isTool(player.getItemInHand(hand)) && denyIfHandcuffed(player)
						? InteractionResult.FAIL : InteractionResult.PASS);
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
				denyIfHandcuffed(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(source.getEntity() instanceof Player attacker && denyIfHandcuffed(attacker)));
	}

	public static boolean isHandcuffed(Player player) {
		return WearableTags.isEquipped(player, "offhand/glove", WearableTags.HANDCUFFS);
	}

	public static void setClientFeedback(Consumer<Player> feedback) {
		clientFeedback = feedback;
	}

	public static boolean denyIfHandcuffed(Player player) {
		if (!isHandcuffed(player)) return false;
		long now = System.currentTimeMillis();
		Long lastMessageTime = LAST_MESSAGE_TIMES.get(player.getUUID());
		if (lastMessageTime == null || now - lastMessageTime >= MESSAGE_INTERVAL_MS) {
			if (player.level().isClientSide()) {
				clientFeedback.accept(player);
			} else {
				player.sendSystemMessage(Component.translatable(MESSAGE_KEY));
			}
			LAST_MESSAGE_TIMES.put(player.getUUID(), now);
		}
		return true;
	}

	private static boolean isTool(ItemStack stack) {
		return stack.get(DataComponents.TOOL) != null;
	}
}