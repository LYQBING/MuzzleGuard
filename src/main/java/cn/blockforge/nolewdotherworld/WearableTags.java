package cn.blockforge.nolewdotherworld;

import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class WearableTags {
	public static final TagKey<Item> GAGS = create("gags");
	public static final TagKey<Item> LOCKABLES = create("lockables");
	public static final TagKey<Item> COLLARS = create("collars");
	public static final TagKey<Item> HANDCUFFS = create("handcuffs");

	private WearableTags() {
	}

	public static ItemStack findEquipped(Player player, String slot, TagKey<Item> tag) {
		TrinketInventory inventory = TrinketsApi.getAttachment(player).getInventory(slot);
		if (inventory == null) return null;
		for (int index = 0; index < inventory.getContainerSize(); index++) {
			ItemStack stack = inventory.getItem(index);
			if (!stack.isEmpty() && stack.is(tag)) return stack;
		}
		return null;
	}

	public static boolean isEquipped(Player player, String slot, TagKey<Item> tag) {
		return findEquipped(player, slot, tag) != null;
	}

	public static boolean isHandcuffed(Player player) {
		return isEquipped(player, "offhand/glove", HANDCUFFS);
	}

	public static boolean isLockable(ItemStack stack) {
		return !stack.isEmpty() && stack.is(LOCKABLES);
	}

	private static TagKey<Item> create(String path) {
		return TagKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(NoLewdOtherworldMod.MOD_ID, path));
	}
}
