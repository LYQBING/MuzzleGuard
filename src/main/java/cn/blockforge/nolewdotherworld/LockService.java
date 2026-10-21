package cn.blockforge.nolewdotherworld;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import dev.yumi.commons.TriState;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.event.TrinketCanUnequipCallback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

public final class LockService {
	private static final String KEY_UUID = "muzzle_guard_key";
	private static final String BOUND_KEY_UUID = "muzzle_guard_bound_key";
	private static final String LOCKED = "muzzle_guard_locked";
	private static final List<String> LOCKABLE_SLOTS = List.of("head/face", "chest/necklace", "offhand/glove");

	private LockService() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("muzzleguarddetails")
					.requires(source -> true)
					.then(Commands.argument("target", EntityArgument.player())
							.executes(context -> showDetails(
									context.getSource().getPlayerOrException(),
									EntityArgument.getPlayer(context, "target"))))));
		TrinketCanUnequipCallback.EVENT.register((stack, slot, entity, canUnequipDefault) -> {
			if (!WearableTags.isLockable(stack)) return TriState.DEFAULT;
			return isLocked(stack) ? TriState.FALSE : TriState.DEFAULT;
		});
	}

	public static void appendWearableTooltip(ItemStack stack, Consumer<Component> textConsumer) {
		if (!WearableTags.isLockable(stack)) return;

		String boundKey = boundKeyId(stack);
		textConsumer.accept(Component.translatable(boundKey.isEmpty()
				? "tooltip.no_lewd_otherworld.unbound"
				: "tooltip.no_lewd_otherworld.bound_key", NoLewdOtherworldMod.shortId(boundKey)));
		if (!boundKey.isEmpty()) {
			textConsumer.accept(Component.translatable(isLocked(stack)
					? "tooltip.no_lewd_otherworld.locked"
					: "tooltip.no_lewd_otherworld.unlocked"));
		}
	}

	public static void appendKeyTooltip(ItemStack stack, Consumer<Component> textConsumer) {
		String id = NoLewdOtherworldMod.customData(stack).getString(KEY_UUID).orElse("");
		if (!id.isEmpty()) {
			textConsumer.accept(Component.translatable("tooltip.no_lewd_otherworld.key_id", NoLewdOtherworldMod.shortId(id)));
		}
	}

	public static boolean isLocked(ItemStack stack) {
		return NoLewdOtherworldMod.customData(stack).getBoolean(LOCKED).orElse(false);
	}

	public static boolean isLockedAndLockable(ItemStack stack) {
		return WearableTags.isLockable(stack) && isLocked(stack);
	}

	public static String boundKeyId(ItemStack stack) {
		return NoLewdOtherworldMod.customData(stack).getString(BOUND_KEY_UUID).orElse("");
	}

	public static boolean hasMatchingKey(Player player, String boundKeyId) {
		if (boundKeyId.isEmpty()) return false;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (isMatchingKey(stack, boundKeyId)) return true;
		}
		for (InteractionHand hand : InteractionHand.values()) {
			if (isMatchingKey(player.getItemInHand(hand), boundKeyId)) return true;
		}
		return false;
	}

	public static boolean bindOrToggle(ItemStack key, Player actor, Player target, boolean addBinding) {
		String id = NoLewdOtherworldMod.customData(key).getString(KEY_UUID).orElse("");
		List<LockCandidate> candidates = findCandidates(target);
		List<LockCandidate> boundCandidates = candidates.stream()
				.filter(candidate -> !candidate.boundKey.isEmpty()).toList();
		List<LockCandidate> matchingCandidates = id.isEmpty() ? List.of() : candidates.stream()
				.filter(candidate -> candidate.boundKey.equals(id)).toList();

		if (!matchingCandidates.isEmpty()) {
			if (addBinding) return bindUnbound(key, id, actor, target, candidates);
			boolean lock = matchingCandidates.stream().noneMatch(candidate ->
					candidate.data.getBoolean(LOCKED).orElse(false));
			for (LockCandidate candidate : matchingCandidates) {
				candidate.data.putBoolean(LOCKED, lock);
				NoLewdOtherworldMod.setCustomData(candidate.stack, candidate.data);
				candidate.inventory.setChanged();
			}
			Component result = Component.translatable(lock
					? "message.no_lewd_otherworld.locked_group"
					: "message.no_lewd_otherworld.unlocked_group", matchingCandidates.size());
			sendDetailsLink(actor, target, result);
			if (!actor.getUUID().equals(target.getUUID())) sendDetailsLink(target, target, result);
			return true;
		}

		if (addBinding) return bindUnbound(key, id, actor, target, candidates);
		if (!boundCandidates.isEmpty()) {
			sendDetailsLink(actor, target,
					Component.translatable("message.no_lewd_otherworld.no_unlockable_trinkets"));
			return false;
		}
		return bindUnbound(key, id, actor, target, candidates);
	}

	private static void sendDetailsLink(Player viewer, Player target, Component message) {
		String command = "/muzzleguarddetails " + target.getScoreboardName();
		viewer.sendSystemMessage(message.copy().append(Component.translatable(
				"message.no_lewd_otherworld.details_link").withStyle(style ->
						style.withColor(ChatFormatting.AQUA)
								.withClickEvent(new ClickEvent.RunCommand(command)).withUnderlined(true))));
	}

	private static int showDetails(Player viewer, Player target) {
		viewer.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.trinket_details_header",
				target.getDisplayName()));
		List<LockCandidate> candidates = findCandidates(target);
		if (candidates.isEmpty()) {
			viewer.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.no_trinkets_worn"));
			return 1;
		}
		for (LockCandidate candidate : candidates) {
			boolean locked = candidate.data.getBoolean(LOCKED).orElse(false);
			String status = candidate.boundKey.isEmpty()
					? "message.no_lewd_otherworld.trinket_unbound"
					: locked ? "message.no_lewd_otherworld.trinket_locked"
							: "message.no_lewd_otherworld.trinket_unlocked";
		ChatFormatting statusColor = candidate.boundKey.isEmpty()
				? ChatFormatting.GRAY
				: locked ? ChatFormatting.RED : ChatFormatting.GREEN;
			viewer.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.trinket_detail_line",
					candidate.stack.getHoverName(), Component.translatable(status).withStyle(statusColor)));
		}
		return 1;
	}

	private static boolean bindUnbound(ItemStack key, String id, Player actor, Player target,
			List<LockCandidate> candidates) {
		LockCandidate selected = candidates.stream()
				.filter(candidate -> candidate.boundKey.isEmpty()).findFirst().orElse(null);
		if (selected == null) {
			sendDetailsLink(actor, target,
					Component.translatable("message.no_lewd_otherworld.no_unlockable_trinkets"));
			return false;
		}

		String keyId = id.isEmpty() ? keyId(key) : id;
		selected.data.putString(BOUND_KEY_UUID, keyId);
		selected.data.putBoolean(LOCKED, true);
		NoLewdOtherworldMod.setCustomData(selected.stack, selected.data);
		selected.inventory.setChanged();

		Component result = Component.translatable("message.no_lewd_otherworld.key_bound_to",
				selected.stack.getHoverName());
		actor.sendSystemMessage(result);
		if (!actor.getUUID().equals(target.getUUID())) target.sendSystemMessage(result);
		return true;
	}

	public static boolean removeBinding(Player actor, Player target) {
		int unlockedCount = 0;
		for (LockCandidate candidate : findCandidates(target)) {
			if (candidate.boundKey.isEmpty() && !candidate.data.getBoolean(LOCKED).orElse(false)) continue;
			candidate.data.remove(BOUND_KEY_UUID);
			candidate.data.remove(LOCKED);
			NoLewdOtherworldMod.setCustomData(candidate.stack, candidate.data);
			candidate.inventory.setChanged();
			unlockedCount++;
		}
		if (unlockedCount == 0) {
			actor.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.key_no_target"));
			return false;
		}
		Component result = Component.translatable("message.no_lewd_otherworld.master_key_unlocked", unlockedCount);
		actor.sendSystemMessage(result);
		if (!actor.getUUID().equals(target.getUUID())) target.sendSystemMessage(result);
		return true;
	}

	private static List<LockCandidate> findCandidates(Player target) {
		List<LockCandidate> candidates = new ArrayList<>();
		for (String slot : LOCKABLE_SLOTS) {
			TrinketInventory inventory = TrinketsApi.getAttachment(target).getInventory(slot);
			if (inventory == null) continue;
			for (int index = 0; index < inventory.getContainerSize(); index++) {
				ItemStack worn = inventory.getItem(index);
				if (WearableTags.isLockable(worn)) candidates.add(new LockCandidate(worn, inventory));
			}
		}
		return candidates;
	}

	private static boolean isMatchingKey(ItemStack stack, String boundKeyId) {
		return stack.is(NoLewdOtherworldMod.KEY)
				&& boundKeyId.equals(NoLewdOtherworldMod.customData(stack).getString(KEY_UUID).orElse(""));
	}

	private static String keyId(ItemStack key) {
		CompoundTag data = NoLewdOtherworldMod.customData(key);
		String id = data.getString(KEY_UUID).orElse("");
		if (id.isEmpty()) {
			id = UUID.randomUUID().toString();
			data.putString(KEY_UUID, id);
			NoLewdOtherworldMod.setCustomData(key, data);
		}
		return id;
	}

	private static final class LockCandidate {
		private final ItemStack stack;
		private final TrinketInventory inventory;
		private final CompoundTag data;
		private final String boundKey;

		private LockCandidate(ItemStack stack, TrinketInventory inventory) {
			this.stack = stack;
			this.inventory = inventory;
			this.data = NoLewdOtherworldMod.customData(stack);
			this.boundKey = data.getString(BOUND_KEY_UUID).orElse("");
		}
	}
}
