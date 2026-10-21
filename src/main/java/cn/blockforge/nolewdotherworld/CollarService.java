package cn.blockforge.nolewdotherworld;

import java.util.UUID;
import java.util.function.Consumer;

import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CollarService {
	private static final String COLLAR_UUID = "muzzle_guard_collar";
	private static final String CONTROLLER_PLAYER_UUID = "muzzle_guard_controller_player";
	private static final String CONTROLLER_COLLAR_UUID = "muzzle_guard_controller_collar";

	private CollarService() {
	}

	public static void appendControllerTooltip(ItemStack stack, Consumer<Component> textConsumer) {
		CompoundTag data = NoLewdOtherworldMod.customData(stack);
		String playerId = data.getString(CONTROLLER_PLAYER_UUID).orElse("");
		String collarId = data.getString(CONTROLLER_COLLAR_UUID).orElse("");
		if (!playerId.isEmpty() && !collarId.isEmpty()) {
			textConsumer.accept(Component.translatable("tooltip.no_lewd_otherworld.controller_bound",
					NoLewdOtherworldMod.shortId(playerId), NoLewdOtherworldMod.shortId(collarId)));
		}
	}

	public static void useController(ItemStack controller, Player user, Player selectedPlayer) {
		CompoundTag controllerData = NoLewdOtherworldMod.customData(controller);
		String playerId = controllerData.getString(CONTROLLER_PLAYER_UUID).orElse("");
		String collarId = controllerData.getString(CONTROLLER_COLLAR_UUID).orElse("");
		if (playerId.isEmpty() || collarId.isEmpty()) {
			ItemStack collar = getEquippedCollar(selectedPlayer);
			if (collar == null) {
				user.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.controller_no_collar"));
				return;
			}
			if (LockService.isLockedAndLockable(collar)
					&& !LockService.hasMatchingKey(user, LockService.boundKeyId(collar))) {
				user.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.controller_collar_locked"));
				return;
			}
			collarId = getOrCreateCollarId(collar);
			markNecklaceInventoryUpdated(selectedPlayer);
			controllerData.putString(CONTROLLER_PLAYER_UUID, selectedPlayer.getStringUUID());
			controllerData.putString(CONTROLLER_COLLAR_UUID, collarId);
			NoLewdOtherworldMod.setCustomData(controller, controllerData);
			user.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.controller_bound",
					selectedPlayer.getDisplayName(), NoLewdOtherworldMod.shortId(collarId)));
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
			NoLewdOtherworldMod.setCustomData(controller, controllerData);
			user.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.controller_unbound"));
			return;
		}

		target.hurtServer((ServerLevel) target.level(), user.damageSources().magic(), 1.0F);
		user.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.controller_shocked", target.getDisplayName()));
		if (!user.getUUID().equals(target.getUUID())) {
			target.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.controller_shocked_target"));
		}
	}

	public static boolean isCollarEquipped(Player player) {
		return getEquippedCollar(player) != null;
	}

	public static boolean canLeashPlayer(Player holder, Player target) {
		ItemStack collar = getEquippedCollar(target);
		if (collar == null) return false;
		if (!LockService.isLockedAndLockable(collar)) return true;
		if (LockService.hasMatchingKey(holder, LockService.boundKeyId(collar))) return true;
		holder.sendSystemMessage(Component.translatable("message.no_lewd_otherworld.controller_collar_locked"));
		return false;
	}

	private static ItemStack getEquippedCollar(Player player) {
		return WearableTags.findEquipped(player, "chest/necklace", WearableTags.COLLARS);
	}

	private static String getEquippedCollarId(Player player) {
		ItemStack collar = getEquippedCollar(player);
		return collar == null ? "" : NoLewdOtherworldMod.customData(collar).getString(COLLAR_UUID).orElse("");
	}

	private static String getOrCreateCollarId(ItemStack collar) {
		CompoundTag data = NoLewdOtherworldMod.customData(collar);
		String id = data.getString(COLLAR_UUID).orElse("");
		if (id.isEmpty()) {
			id = UUID.randomUUID().toString();
			data.putString(COLLAR_UUID, id);
			NoLewdOtherworldMod.setCustomData(collar, data);
		}
		return id;
	}

	private static void markNecklaceInventoryUpdated(Player player) {
		TrinketInventory inventory = TrinketsApi.getAttachment(player).getInventory("chest/necklace");
		if (inventory != null) inventory.setChanged();
	}
}
