package cn.blockforge.nolewdotherworld;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class WorldCreationGate {
	private static final String CONFIG_KEY = "allowWorldCreation";
	private static final boolean ALLOWED = loadAllowed();

	private WorldCreationGate() {
	}

	public static boolean isAllowed() {
		return ALLOWED;
	}

	private static boolean loadAllowed() {
		Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve("no_lewd_otherworld");
		Path configPath = configDirectory.resolve("world_creation.properties");
		Path legacyConfigPath = FabricLoader.getInstance().getConfigDir().resolve("no_lewd_otherworld.properties");
		Properties properties = new Properties();
		try {
			Files.createDirectories(configDirectory);
			if (Files.notExists(configPath)) {
				if (Files.exists(legacyConfigPath)) {
					try (InputStream input = Files.newInputStream(legacyConfigPath)) {
						properties.load(input);
					}
				} else {
					properties.setProperty(CONFIG_KEY, "false");
				}
				try (OutputStream output = Files.newOutputStream(configPath)) {
					properties.store(output, null);
				}
				Files.deleteIfExists(legacyConfigPath);
				return Boolean.parseBoolean(properties.getProperty(CONFIG_KEY, "false"));
			}
			try (InputStream input = Files.newInputStream(configPath)) {
				properties.load(input);
			}
			return Boolean.parseBoolean(properties.getProperty(CONFIG_KEY, "false"));
		} catch (IOException exception) {
			return false;
		}
	}
}
