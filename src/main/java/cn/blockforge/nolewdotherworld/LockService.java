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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LockService {
	private static final String KEY_UUID = "muzzle_guard_key";
	private static final String BOUND_KEY_UUID = "muzzle_guard_bound_key";
	private static final String LOCKED = "muzzle_guard_locked";
	private static final List<String> LOCKABLE_SLOTS = List.of("head/face", "chest/necklace", "offhand/glove");

	private LockService() {
	}

	public static void register() {
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

	public static boolean bindOrToggle(ItemStack key, Player actor, Player target) {
		String id = NoLewdOtherworldMod.customData(key).getString(KEY_UUID).orElse("");
		String boundKeyId = id;
		List<LockCandidate> candidates = findCandidates(target);
		LockCandidate selected = boundKeyId.isEmpty() ? null : candidates.stream()
				.filter(candidate -> candidate.boundKey.equals(boundKeyId))
				.findFirst().orElse(null);
		if (selected == null) {
			selected = candidates.stream().filter(candidate -> candidate.boundKey.isEmpty()).findFirst().orElse(null);
		}
		if (selected == null) {
			if (candidates.isEmpty()) {
				actor.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.key_no_target"));
			} else {
				actor.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.key_bound_elsewhere", candidates.getFirst().stack.getHoverName()));
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
		NoLewdOtherworldMod.setCustomData(selected.stack, selected.data);
		selected.inventory.setChanged();

		String message = newlyBound
				? "message.no_lewd_otherworld.key_bound_to"
				: isLocked(selected.stack)
						? "message.no_lewd_otherworld.locked_item"
						: "message.no_lewd_otherworld.unlocked_item";
		Component result = Component.translatable(message, selected.stack.getHoverName());
		actor.sendSystemMessage(result);
		if (!actor.getUUID().equals(target.getUUID())) target.sendSystemMessage(result);
		return true;
	}

	public static boolean removeBinding(Player actor, Player target) {
		TrinketAttachment attachment = TrinketsApi.getAttachment(target);
		for (String slot : LOCKABLE_SLOTS) {
			TrinketInventory inventory = attachment.getInventory(slot);
			if (inventory == null) continue;
			for (int index = 0; index < inventory.getContainerSize(); index++) {
				ItemStack worn = inventory.getItem(index);
				if (!WearableTags.isLockable(worn)) continue;
				CompoundTag data = NoLewdOtherworldMod.customData(worn);
				data.remove(BOUND_KEY_UUID);
				data.remove(LOCKED);
				NoLewdOtherworldMod.setCustomData(worn, data);
				inventory.setChanged();
				Component result = Component.translatable("message.no_lewd_otherworld.master_key_unlocked");
				actor.sendSystemMessage(result);
				if (!actor.getUUID().equals(target.getUUID())) target.sendSystemMessage(result);
				return true;
			}
		}
		actor.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.key_no_target"));
		return false;
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
