package cn.blockforge.nolewdotherworld;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

public final class HandcuffWhitelist {
	private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
			.resolve(NoLewdOtherworldMod.MOD_ID)
			.resolve("handcuff_command_whitelist.txt");

	private static final List<String> DEFAULTS = List.of(
			"login",
			"l",
			"register",
			"changepassword",
			"logout",
			"muzzleguarddetails"
	);

	private static volatile Set<String> allowedCommands = load();
	private static volatile long lastModified = fileLastModified();

	private HandcuffWhitelist() {
	}

	public static boolean isCommandAllowed(String command) {
		reloadIfChanged();
		return command != null && allowedCommands.contains(commandName(command));
	}

	private static void reloadIfChanged() {
		long modified = fileLastModified();
		if (modified != lastModified) {
			allowedCommands = load();
			lastModified = modified;
		}
	}

	private static long fileLastModified() {
		try {
			return Files.exists(CONFIG_FILE)
					? Files.getLastModifiedTime(CONFIG_FILE).toMillis()
					: Long.MIN_VALUE;
		} catch (IOException exception) {
			return Long.MIN_VALUE;
		}
	}

	private static Set<String> load() {
		Set<String> commands = new LinkedHashSet<>();
		if (!Files.exists(CONFIG_FILE)) {
			writeDefaults();
		}
		try {
			for (String line : Files.readAllLines(CONFIG_FILE, StandardCharsets.UTF_8)) {
				String name = commandName(line);
				if (!name.isEmpty()) commands.add(name);
			}
		} catch (IOException exception) {
			return new LinkedHashSet<>(DEFAULTS);
		}
		if (commands.isEmpty()) commands.addAll(DEFAULTS);
		return commands;
	}

	private static void writeDefaults() {
		try {
			Files.createDirectories(CONFIG_FILE.getParent());
			List<String> lines = new ArrayList<>();
			lines.add("# Commands allowed while handcuffed (one per line, case-insensitive).");
			lines.addAll(DEFAULTS);
			Files.write(CONFIG_FILE, lines, StandardCharsets.UTF_8);
		} catch (IOException ignored) {
		}
	}

	private static String commandName(String command) {
		String text = command.trim();
		if (text.startsWith("#")) return "";
		if (text.startsWith("/")) text = text.substring(1);
		int space = text.indexOf(' ');
		String name = space == -1 ? text : text.substring(0, space);
		return name.toLowerCase(Locale.ROOT);
	}
}
