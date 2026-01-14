package dev.firee.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.firee.TpaPlugin;

import java.util.UUID;

import javax.annotation.Nonnull;

public class TpdenyCommand extends AbstractPlayerCommand {

    public TpdenyCommand() {
        super("tpdeny", "Denies a teleportation request sent from a player.");
    }
    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        UUID fromPlayer = TpaPlugin.get().manager.getOtherPlayer(playerRef.getUuid(), false, false);
        UUID fromPlayerHere = TpaPlugin.get().manager.getOtherPlayer(playerRef.getUuid(), true, false);
        if (fromPlayer == null && fromPlayerHere == null) {
            context.sendMessage(Message.raw("You don't have any teleportation requests!"));
            return;
        }
        PlayerRef playerFrom;
        if (fromPlayer != null) {
            playerFrom = Universe.get().getPlayer(fromPlayer);
            TpaPlugin.get().manager.removeRequest(fromPlayer, false);
        } else {
            playerFrom = Universe.get().getPlayer(fromPlayerHere);
            TpaPlugin.get().manager.removeRequest(fromPlayerHere, true);
        }
        playerFrom.sendMessage(Message.raw(String.format("Your teleport request to '%s' has been denied.", playerRef.getUsername())));
        context.sendMessage(Message.raw(String.format("The teleport request for '%s' has been denied.", playerFrom.getUsername())));
    }
}
