
package dev.firee;

import java.util.UUID;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
public class Manager {
    private final TpaPlugin plugin;
    private int duration = 3;
    private final Map<UUID, UUID> tpaRequests = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> tpahereRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private Set<UUID> tpaDisabled = new HashSet<>();
    // private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>(); // doing /back next update

    public Manager(TpaPlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().atInfo().log("Initialized Manager");
        if (load("config.json")) {
            plugin.getLogger().atInfo().log("Loaded tptoggled.json");
        } else {
            plugin.getLogger().atWarning().log("Couldn't load tptoggled.json");
        }
    }

    public boolean load(String fileName) {
        try {
            Path file = plugin.getDataDirectory().resolve(fileName);
            if (!Files.exists(file)) {
                save(fileName);
                plugin.getLogger().atInfo().log("tptoggled.json doesn't exist, creating");
                return true;
            }
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
            
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile())));
			ConfigData data = gson.fromJson(reader, ConfigData.class);
            tpaDisabled = data.tpatoggle;
            duration = data.duration;
            return true;
		} catch (IOException any) {
			return false;
		}
	}
    public boolean save(String fileName) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Path dataFolder = plugin.getDataDirectory();
            Path file = plugin.getDataDirectory().resolve(fileName);
            ConfigData data = new ConfigData();
            data.tpatoggle = Set.copyOf(tpaDisabled);
            data.duration = duration;
            Files.createDirectories(dataFolder);
            String jsonConfig = gson.toJson(data);
            Files.writeString(file, jsonConfig);
            return true;
        } catch (IOException any) {
            return false;
        }
    }

    public long checkCooldown(UUID playerId) {
        long now = System.currentTimeMillis();
        if (!cooldowns.containsKey(playerId)) {
            cooldowns.put(playerId, now);
        } else {
            long expiration = cooldowns.getOrDefault(playerId, 0L);
            if (now < expiration) {
                return (expiration - now) / 1000L;
            }
        }
        return 0L;
    }
    public boolean isIgnoringRequests(UUID playerId) {
        return tpaDisabled.contains(playerId);
    }
    public boolean toggleTpa(UUID playerId) {
        if (tpaDisabled.remove(playerId)) {
            return false;
        }
        tpaDisabled.add(playerId);
        return true;
    }
    public boolean sendTPARequest(UUID playerFrom, UUID playerTo, boolean teleportHere) {
        if (tpaRequests.containsKey(playerFrom) || tpahereRequests.containsKey(playerFrom)) {
            return false;
        }
        if (teleportHere) {
            tpahereRequests.put(playerTo, playerFrom);
        } else {
            tpaRequests.put(playerTo, playerFrom);
        }
        return true;
    }
    public void removeRequest(UUID key, boolean teleportHere) {
        if (teleportHere) {
            tpaRequests.remove(key);
        } else {
            tpaRequests.remove(key);
        }
    }
    public UUID getOtherPlayer(UUID player, boolean teleportHere, boolean cancel) {
        Map<UUID, UUID> map = teleportHere ? tpahereRequests : tpaRequests;
        if (cancel) {
            if (map.containsValue(player)) {
                for (var entry : map.entrySet()) {
                    if (entry.getValue().equals(player)) {
                        return entry.getKey();
                    }
                }
            } else if (map.containsKey(player)) {
                for (var entry : map.entrySet()) {
                    if (entry.getKey().equals(player)) {
                        return entry.getValue();
                    }
                }
            }
        } else {
            if (map.containsValue(player)) {
                for (var entry : map.entrySet()) {
                    if (entry.getValue().equals(player)) {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
    }
    public Message scheduleTeleportReq(@Nonnull World fromWorld, UUID playerFrom, UUID playerTo, boolean teleportHere, boolean instant) {
        PlayerRef playerFromRef = Universe.get().getPlayer(playerFrom);
        PlayerRef playerToRef = Universe.get().getPlayer(playerTo);
        if (playerFromRef == null || playerToRef == null) {
            return Message.raw("One or both players do not exist.");
        }
        Ref<EntityStore> targetRef = (teleportHere) ? playerFromRef.getReference() : playerToRef.getReference();
        if (targetRef == null || !targetRef.isValid()) {
            return Message.translation("server.commands.errors.targetNotInWorld");
        }
        Store<EntityStore> targetStore = targetRef.getStore();
        World targetWorld = targetStore.getExternalData().getWorld();
        TransformComponent targetTransformComponent = targetStore.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetTransformComponent == null) return Message.translation("server.commands.errors.targetNotInWorld");

        final Vector3d oldTargetPosition = targetTransformComponent.getPosition().clone();
        final Vector3f oldTargetRotation = targetTransformComponent.getRotation().clone();
        ScheduledFuture<Void> scheduledTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> {
                targetWorld.execute(() -> {
                    if (targetRef == null || !targetRef.isValid()) {
                        return;
                    }
                    TransformComponent currentTransform = targetStore.getComponent(targetRef, TransformComponent.getComponentType());
                    if (currentTransform == null) return;
                    double distanceTo = currentTransform.getPosition().distanceTo(oldTargetPosition);
                    if (distanceTo > 0) {
                        plugin.getLogger().atInfo().log("Teleport request was cancelled due to movement (%f blocks)", distanceTo);
                        Message to = Message.raw("Teleport cancelled due to movement!");
                        Message from = Message.raw("The teleport has been cancelled because the other player moved.");
                        if (teleportHere) {
                            playerToRef.sendMessage(to);
                            playerFromRef.sendMessage(from);
                        } else {
                            playerFromRef.sendMessage(to);
                            playerToRef.sendMessage(from);
                        }
                        return;
                    }
                    PlayerRef n_playerFromRef = Universe.get().getPlayer(playerFrom);
                    PlayerRef n_playerToRef = Universe.get().getPlayer(playerTo);
                    if (n_playerFromRef == null || n_playerToRef == null) return;
                    plugin.getLogger().atInfo().log(
                        "Teleported '%s' to '%s' (%f, %f, %f)",
                        playerFromRef.getUsername(),
                        playerToRef.getUsername(),
                        oldTargetPosition.getX(),
                        oldTargetPosition.getY(),
                        oldTargetPosition.getZ()
                    );
                    if (teleportHere) {
                        targetStore.addComponent(targetRef, Teleport.getComponentType(), new Teleport(targetWorld, oldTargetPosition, oldTargetRotation));
                        n_playerFromRef.sendMessage(Message.raw(String.format("You have been teleported to '%s'.", playerToRef.getUsername())));
                    } else {
                        targetStore.addComponent(targetRef, Teleport.getComponentType(), new Teleport(fromWorld, oldTargetPosition, oldTargetRotation));
                        n_playerToRef.sendMessage(Message.raw(String.format("You have been teleported to '%s'.", playerFromRef.getUsername())));
                    }
                });
                return null;
            },
            duration, TimeUnit.SECONDS
        );
        plugin.getTaskRegistry().registerTask(scheduledTask);

        if (teleportHere) {
            playerToRef.sendMessage(Message.raw(String.format("You will be teleported in %d duration! Do not move!", duration)));
            return Message.raw(String.format("'%s' will be teleported in %d duration! Do not move!", playerToRef.getUsername(), duration));
        } else {
            return Message.raw(String.format("Teleporting in %d duration! Do not move!", duration));
        }
    }
}

