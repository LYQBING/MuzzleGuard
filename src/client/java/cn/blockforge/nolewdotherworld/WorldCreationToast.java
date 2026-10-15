package cn.blockforge.nolewdotherworld;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;

public final class WorldCreationToast {
	private static final Component MESSAGE = Component.literal("当前环境不满足世界创建的基本要求");

	private WorldCreationToast() {
	}

	public static void show(Minecraft minecraft) {
		ToastManager toastManager = findToastManager(minecraft);
		if (toastManager != null) {
			SystemToast.add(toastManager, SystemToast.SystemToastId.PERIODIC_NOTIFICATION, MESSAGE, Component.empty());
		}
	}

	private static ToastManager findToastManager(Minecraft minecraft) {
		for (Class<?> type = Minecraft.class; type != null; type = type.getSuperclass()) {
			for (Method method : type.getDeclaredMethods()) {
				if (method.getParameterCount() == 0 && ToastManager.class.isAssignableFrom(method.getReturnType())) {
					try {
						if (method.trySetAccessible()) {
							return (ToastManager) method.invoke(Modifier.isStatic(method.getModifiers()) ? null : minecraft);
						}
					} catch (ReflectiveOperationException | RuntimeException ignored) {
					}
				}
			}
			for (Field field : type.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) || !ToastManager.class.isAssignableFrom(field.getType())) continue;
				try {
					if (field.trySetAccessible()) return (ToastManager) field.get(minecraft);
				} catch (IllegalAccessException | RuntimeException ignored) {
				}
			}
		}
		return null;
	}
}
