package dev.firee.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.firee.TpaPlugin;
import javax.annotation.Nonnull;

public class TpaCommand extends AbstractPlayerCommand {

    private final RequiredArg<PlayerRef> playerArg;

    public TpaCommand() {
        super("tpa", "Sends a teleportation request to a specified player.");
        playerArg = withRequiredArg("player", "The target player to send a request to", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        PlayerRef player = playerArg.get(context);
        Ref<EntityStore> target = player.getReference();
        if (target == null || !target.isValid()) {
            context.sendMessage(Message.translation("server.commands.errors.targetNotInWorld"));
            return;
        }
        if (TpaPlugin.get().manager.isIgnoringRequests(player.getUuid())) {
            context.sendMessage(Message.raw(String.format("'%s' has teleport requests disabled.", player.getUsername())));
            return;
        }
        if (TpaPlugin.get().manager.sendTPARequest(playerRef.getUuid(), player.getUuid(), false)) {
            context.sendMessage(Message.raw(String.format("A teleport request was sent to '%s'!", player.getUsername())));
            player.sendMessage(Message.raw(String.format("'%s' has requested to teleport to you! Type /tpaccept to accept, or /tpdeny to deny.", playerRef.getUsername())));
        } else {
            context.sendMessage(Message.raw("You already have a pending teleport request!"));
        }
    }
}
