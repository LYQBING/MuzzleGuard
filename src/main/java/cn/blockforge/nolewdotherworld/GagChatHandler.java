package cn.blockforge.nolewdotherworld;

import java.util.PrimitiveIterator;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class GagChatHandler {
	private static final String[] MUFFLED_SYLLABLES = {"呜", "啊", "哇", "呀", "嗯", "哼", "唔", "哦", "噢", "诶", "欸", "哎", "咿", "嘤", "喵"};

	private GagChatHandler() {
	}

	public static void register() {
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> onMessage(message.signedContent(), sender));
	}

	private static boolean onMessage(String content, Player sender) {
		if (!WearableTags.isEquipped(sender, "head/face", WearableTags.GAGS)) return true;

		String muffledContent = muffle(content);
		if (muffledContent == null) return true;

		Component muffledMessage = sender.getDisplayName().copy()
				.append(Component.literal(": "))
				.append(Component.literal(muffledContent));
		sender.level().getServer().getPlayerList().broadcastSystemMessage(muffledMessage, false);
		return false;
	}

	private static String muffle(String content) {
		StringBuilder muffled = new StringBuilder();
		boolean changed = false;
		PrimitiveIterator.OfInt codePoints = content.codePoints().iterator();
		while (codePoints.hasNext()) {
			int codePoint = codePoints.nextInt();
			if (Character.isLetterOrDigit(codePoint) && !isMuffledSyllable(codePoint)) {
				muffled.append(MUFFLED_SYLLABLES[ThreadLocalRandom.current().nextInt(MUFFLED_SYLLABLES.length)]);
				changed = true;
			} else {
				muffled.appendCodePoint(codePoint);
			}
		}
		return changed ? muffled.toString() : null;
	}

	private static boolean isMuffledSyllable(int codePoint) {
		for (String syllable : MUFFLED_SYLLABLES) {
			if (syllable.codePointAt(0) == codePoint) return true;
		}
		return false;
	}
}
