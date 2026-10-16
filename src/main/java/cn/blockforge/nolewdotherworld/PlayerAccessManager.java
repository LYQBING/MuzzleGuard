package cn.blockforge.nolewdotherworld;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public final class PlayerAccessManager {
	private static final ResourceKey<MobEffect> ENERGY_RELIEF_KEY = ResourceKey.create(
			Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath("needsofnature", "energy_relief"));
	private static final int EFFECT_DURATION = 1200;
	private static final int EFFECT_REFRESH_THRESHOLD = 200;
	private static final Set<UUID> AUTHORIZED_PLAYERS = new HashSet<>();
	private static Path storageFile;
	private static int effectRefreshTicks;

	private PlayerAccessManager() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			updateEffect(handler.player);
			sendIdentityMessage(handler.player);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (++effectRefreshTicks % 100 != 0) return;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) updateEffect(player);
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("nonaccess")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(Commands.literal("set")
								.then(Commands.argument("player", EntityArgument.player())
									.then(Commands.literal("on").executes(context -> setAccess(EntityArgument.getPlayer(context, "player"), true, context.getSource())))
									.then(Commands.literal("off").executes(context -> setAccess(EntityArgument.getPlayer(context, "player"), false, context.getSource())))))
						.then(Commands.literal("check")
								.then(Commands.argument("player", EntityArgument.player())
										.executes(context -> checkAccess(EntityArgument.getPlayer(context, "player"), context.getSource()))))));
	}

	public static boolean isAuthorized(UUID playerId) {
		return AUTHORIZED_PLAYERS.contains(playerId);
	}

	public static boolean isAuthorized(ServerPlayer player) {
		return isAuthorized(player.getUUID());
	}

	private static int setAccess(ServerPlayer player, boolean authorized, net.minecraft.commands.CommandSourceStack source) {
		if (authorized) {
			AUTHORIZED_PLAYERS.add(player.getUUID());
		} else {
			AUTHORIZED_PLAYERS.remove(player.getUUID());
		}
		updateEffect(player);
		save();
		sendIdentityMessage(player);
		source.sendSuccess(() -> Component.translatable(
				authorized ? "message.no_lewd_otherworld.access_enabled" : "message.no_lewd_otherworld.access_disabled",
				player.getDisplayName()), true);
		return 1;
	}

	private static void sendIdentityMessage(ServerPlayer player) {
		player.sendSystemMessage(Component.translatable(
				isAuthorized(player) ? "message.no_lewd_otherworld.login_unrestricted" : "message.no_lewd_otherworld.login_standard"));
	}

	private static void updateEffect(ServerPlayer player) {
		Holder<MobEffect> energyRelief = BuiltInRegistries.MOB_EFFECT.getHolder(ENERGY_RELIEF_KEY)
				.orElseThrow(() -> new IllegalStateException("Needs of Nature energy relief effect is not registered"));
		if (isAuthorized(player)) {
			player.removeEffect(energyRelief);
			return;
		}
		MobEffectInstance activeEffect = player.getEffect(energyRelief);
		if (activeEffect == null || activeEffect.getDuration() <= EFFECT_REFRESH_THRESHOLD) {
			player.addEffect(new MobEffectInstance(energyRelief, EFFECT_DURATION, 0, true, false, false));
		}
	}

	private static int checkAccess(ServerPlayer player, net.minecraft.commands.CommandSourceStack source) {
		source.sendSuccess(() -> Component.translatable(
				isAuthorized(player) ? "message.no_lewd_otherworld.access_status_unrestricted" : "message.no_lewd_otherworld.access_status_standard",
				player.getDisplayName()), false);
		return 1;
	}

	private static void load() {
		storageFile = getStorageFile();
		AUTHORIZED_PLAYERS.clear();
		loadAuthorizedPlayers(storageFile);
	}

	private static Path getStorageFile() {
		return FabricLoader.getInstance().getConfigDir()
				.resolve(NoLewdOtherworldMod.MOD_ID)
				.resolve("authorized_players.txt");
	}

	private static void loadAuthorizedPlayers(Path file) {
		if (!Files.exists(file)) return;
		try {
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				try {
					AUTHORIZED_PLAYERS.add(UUID.fromString(line.trim()));
				} catch (IllegalArgumentException ignored) {
				}
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Could not load authorized player data", exception);
		}
	}

	private static void save() {
		if (storageFile == null) {
			storageFile = getStorageFile();
		}
		try {
			Files.createDirectories(storageFile.getParent());
			Files.write(storageFile, AUTHORIZED_PLAYERS.stream().map(UUID::toString).sorted().toList(), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new IllegalStateException("Could not save authorized player data", exception);
		}
	}
}
