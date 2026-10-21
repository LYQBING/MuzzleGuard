package cn.blockforge.nolewdotherworld;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class HandcuffRestrictionService {
	private HandcuffRestrictionService() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
				isHandcuffed(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
				isHandcuffed(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
				!isHandcuffed(player));
		UseItemCallback.EVENT.register((player, world, hand) ->
				isHandcuffed(player) && isTool(player.getItemInHand(hand))
						? InteractionResult.FAIL : InteractionResult.PASS);
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
				isHandcuffed(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(source.getEntity() instanceof Player attacker && isHandcuffed(attacker)));
	}

	public static boolean isHandcuffed(Player player) {
		return WearableTags.isEquipped(player, "offhand/glove", WearableTags.HANDCUFFS);
	}

	private static boolean isTool(ItemStack stack) {
		return stack.get(DataComponents.TOOL) != null;
	}
}