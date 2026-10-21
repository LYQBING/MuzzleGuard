package cn.blockforge.nolewdotherworld;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class ShockControllerItem extends Item {
	public ShockControllerItem(Item.Properties settings) {
		super(settings);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> textConsumer, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, textConsumer, flag);
		CollarService.appendControllerTooltip(stack, textConsumer);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (!level.isClientSide()) CollarService.useController(user.getItemInHand(hand), user, user);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
		if (!user.level().isClientSide() && entity instanceof Player target) {
			CollarService.useController(user.getItemInHand(hand), user, target);
		}
		return InteractionResult.SUCCESS;
	}
}
