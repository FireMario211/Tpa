package dev.firee.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.firee.TpaPlugin;

import java.util.UUID;

import javax.annotation.Nonnull;

public class TpacceptCommand extends AbstractPlayerCommand {

    public TpacceptCommand() {
        super("tpaccept", "Accepts a teleportation request sent from a player.");
    }
    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        UUID fromPlayer = TpaPlugin.get().manager.getOtherPlayer(playerRef.getUuid(), false, false);
        UUID fromPlayerHere = TpaPlugin.get().manager.getOtherPlayer(playerRef.getUuid(), true, false);
        if (fromPlayer == null && fromPlayerHere == null) {
            context.sendMessage(Message.raw("You don't have any teleportation requests!"));
            return;
        }
        if (fromPlayer != null) {
            TpaPlugin.get().manager.removeRequest(fromPlayer, false);
            context.sendMessage(TpaPlugin.get().manager.scheduleTeleportReq(world, fromPlayer, playerRef.getUuid(), false, false));
        } else {
            TpaPlugin.get().manager.removeRequest(fromPlayerHere, true);
            context.sendMessage(TpaPlugin.get().manager.scheduleTeleportReq(world, fromPlayerHere, playerRef.getUuid(), true, false));
        }
    }
}
