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
	private static final String AUTHORIZATION_LIST_KEY = "enableAuthorizationList";
	private static final Properties CONFIG = loadConfig();
	private static final boolean ALLOWED = Boolean.parseBoolean(CONFIG.getProperty(CONFIG_KEY, "false"));
	private static final boolean AUTHORIZATION_LIST_ENABLED = Boolean.parseBoolean(CONFIG.getProperty(AUTHORIZATION_LIST_KEY, "true"));

	private WorldCreationGate() {
	}

	public static boolean isAllowed() {
		return ALLOWED;
	}

	public static boolean isAuthorizationListEnabled() {
		return AUTHORIZATION_LIST_ENABLED;
	}

	private static Properties loadConfig() {
		Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve("no_lewd_otherworld");
		Path configPath = configDirectory.resolve("world_creation.properties");
		Path legacyConfigPath = FabricLoader.getInstance().getConfigDir().resolve("no_lewd_otherworld.properties");
		Properties properties = new Properties();
		try {
			Files.createDirectories(configDirectory);
			boolean configExists = Files.exists(configPath);
			if (configExists) {
				try (InputStream input = Files.newInputStream(configPath)) {
					properties.load(input);
				}
			} else if (Files.exists(legacyConfigPath)) {
				try (InputStream input = Files.newInputStream(legacyConfigPath)) {
					properties.load(input);
				}
			}
			boolean needsSave = !configExists;
			if (!properties.containsKey(CONFIG_KEY)) {
				properties.setProperty(CONFIG_KEY, "false");
				needsSave = true;
			}
			if (!properties.containsKey(AUTHORIZATION_LIST_KEY)) {
				properties.setProperty(AUTHORIZATION_LIST_KEY, "true");
				needsSave = true;
			}
			if (needsSave) {
				try (OutputStream output = Files.newOutputStream(configPath)) {
					properties.store(output, null);
				}
				if (!configExists) Files.deleteIfExists(legacyConfigPath);
			}
			return properties;
		} catch (IOException exception) {
			properties.setProperty(CONFIG_KEY, "false");
			properties.setProperty(AUTHORIZATION_LIST_KEY, "true");
			return properties;
		}
	}
}
