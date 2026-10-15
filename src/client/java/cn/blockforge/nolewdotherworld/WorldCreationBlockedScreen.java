package cn.blockforge.nolewdotherworld;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class WorldCreationBlockedScreen extends Screen {
	private static final Component MESSAGE = Component.literal("当前环境不满足世界创建的基本要求");

	public WorldCreationBlockedScreen() {
		super(MESSAGE);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, MESSAGE, this.width / 2, this.height / 2, 0xFFFFFFFF);
	}
}
