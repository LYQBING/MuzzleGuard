package cn.blockforge.nolewdotherworld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import cn.blockforge.nolewdotherworld.mixin.client.MinecraftAccessor;

public final class WorldCreationToast {
	private static final Component MESSAGE = Component.literal("当前环境不满足世界创建的基本要求");

	private WorldCreationToast() {
	}

	public static void show(Minecraft minecraft) {
		ToastManager toastManager = ((MinecraftAccessor) (Object) minecraft).noLewdOtherworld$getToastManager();
		SystemToast.add(toastManager, SystemToast.SystemToastId.PERIODIC_NOTIFICATION, MESSAGE, Component.empty());
	}
}
