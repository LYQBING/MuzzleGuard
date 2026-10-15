package cn.blockforge.nolewdotherworld;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public final class NonPermissionManager {
	private static final String ENERGY_TAG = "non.energy_stabilized";
	private static final String OWNED_ENERGY_TAG = "no_lewd_otherworld.restricted_energy_tag";
	private static final String OWNED_EFFECT_TAG = "no_lewd_otherworld.restricted_energy_effect";
	private static final int EFFECT_AMPLIFIER = 9;
	private static final ResourceKey<MobEffect> ENERGY_RELIEF = ResourceKey.create(
			Registries.MOB_EFFECT,
			Identifier.fromNamespaceAndPath("needsofnature", "energy_relief")
	);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Set<UUID> ALLOWED_PLAYERS = new HashSet<>();
	private static final Path PERMISSIONS_FILE = FabricLoader.getInstance().getConfigDir()
			.resolve("no_lewd_otherworld")
			.resolve("non_permissions.json");

	private NonPermissionManager() {
	}

	public static void initialize() {
		loadPermissions();
		registerCommands();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> updatePlayer(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.player;
			if (isAllowed(player.getUUID())) {
				clearRestrictedState(player);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				updatePlayer(player);
			}
		});
	}

	public static boolean isAllowed(UUID playerId) {
		return ALLOWED_PLAYERS.contains(playerId);
	}

	private static void updatePlayer(ServerPlayer player) {
		if (isAllowed(player.getUUID())) {
			clearRestrictedState(player);
		} else {
			applyRestrictedState(player);
		}
	}

	private static void applyRestrictedState(ServerPlayer player) {
		if (!player.getTags().contains(ENERGY_TAG)) {
			if (player.addTag(ENERGY_TAG)) {
				player.addTag(OWNED_ENERGY_TAG);
			}
		}

		MobEffectInstance current = player.getEffect(getEnergyRelief(player.serverLevel()));
		if (current == null || current.getAmplifier() < EFFECT_AMPLIFIER || current.getDuration() != MobEffectInstance.INFINITE_DURATION) {
			boolean added = player.addEffect(new MobEffectInstance(
					getEnergyRelief(player.serverLevel()),
					MobEffectInstance.INFINITE_DURATION,
					EFFECT_AMPLIFIER,
					false,
					false,
					false
			));
			if (added && !player.getTags().contains(OWNED_EFFECT_TAG)) {
				player.addTag(OWNED_EFFECT_TAG);
			}
		}
	}

	private static void clearRestrictedState(ServerPlayer player) {
		if (player.getTags().contains(OWNED_EFFECT_TAG)) {
			MobEffectInstance current = player.getEffect(getEnergyRelief(player.serverLevel()));
			if (current != null && current.getAmplifier() == EFFECT_AMPLIFIER && current.getDuration() == MobEffectInstance.INFINITE_DURATION) {
				player.removeEffect(getEnergyRelief(player.serverLevel()));
			}
			player.removeTag(OWNED_EFFECT_TAG);
		}
		if (player.getTags().contains(OWNED_ENERGY_TAG)) {
			player.removeTag(ENERGY_TAG);
			player.removeTag(OWNED_ENERGY_TAG);
		}
	}

	private static Holder<MobEffect> getEnergyRelief(ServerLevel level) {
		return level.registryAccess().lookupOrThrow(Registries.MOB_EFFECT).getOrThrow(ENERGY_RELIEF);
	}

	private static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("non")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("permission")
								.then(Commands.literal("enable")
										.then(Commands.argument("player", EntityArgument.player())
											.executes(context -> setPermission(context.getSource(), EntityArgument.getPlayer(context, "player"), true))))
								.then(Commands.literal("disable")
										.then(Commands.argument("player", EntityArgument.player())
											.executes(context -> setPermission(context.getSource(), EntityArgument.getPlayer(context, "player"), false))))
								.then(Commands.literal("check")
										.then(Commands.argument("player", EntityArgument.player())
											.executes(context -> checkPermission(context.getSource(), EntityArgument.getPlayer(context, "player"))))))
		));
	}

	private static int setPermission(CommandSourceStack source, ServerPlayer player, boolean allowed) {
		boolean changed = allowed ? ALLOWED_PLAYERS.add(player.getUUID()) : ALLOWED_PLAYERS.remove(player.getUUID());
		if (!savePermissions()) {
			if (changed) {
				if (allowed) ALLOWED_PLAYERS.remove(player.getUUID());
				else ALLOWED_PLAYERS.add(player.getUUID());
			}
			source.sendFailure(Component.literal("无法保存 non 权限名单，设置未生效。"));
			return 0;
		}
		updatePlayer(player);
		source.sendSuccess(() -> Component.literal(player.getGameProfile().name() + (allowed ? " 已允许使用 non。" : " 已禁止使用 non。")), true);
		return 1;
	}

	private static int checkPermission(CommandSourceStack source, ServerPlayer player) {
		boolean allowed = isAllowed(player.getUUID());
		source.sendSuccess(() -> Component.literal(player.getGameProfile().name() + (allowed ? "：已允许使用 non。" : "：未允许使用 non。")), false);
		return 1;
	}

	private static void loadPermissions() {
		ALLOWED_PLAYERS.clear();
		if (Files.notExists(PERMISSIONS_FILE)) return;
		try (Reader reader = Files.newBufferedReader(PERMISSIONS_FILE)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray players = root.getAsJsonArray("allowedPlayers");
			if (players == null) return;
			for (JsonElement player : players) {
				try {
					ALLOWED_PLAYERS.add(UUID.fromString(player.getAsString()));
				} catch (IllegalArgumentException ignored) {
				}
			}
		} catch (IOException | RuntimeException exception) {
			System.err.println("[No Lewd Otherworld] Could not load non permission list: " + exception);
		}
	}

	private static boolean savePermissions() {
		try {
			Files.createDirectories(PERMISSIONS_FILE.getParent());
			JsonObject root = new JsonObject();
			JsonArray players = new JsonArray();
			for (UUID player : ALLOWED_PLAYERS) {
				players.add(player.toString());
			}
			root.add("allowedPlayers", players);
			try (Writer writer = Files.newBufferedWriter(PERMISSIONS_FILE)) {
				GSON.toJson(root, writer);
			}
			return true;
		} catch (IOException exception) {
			System.err.println("[No Lewd Otherworld] Could not save non permission list: " + exception);
			return false;
		}
	}
}
