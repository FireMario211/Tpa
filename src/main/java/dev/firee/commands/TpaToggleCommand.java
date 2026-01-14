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
import javax.annotation.Nonnull;

public class TpaToggleCommand extends AbstractPlayerCommand {

    public TpaToggleCommand() {
        super("tpatoggle", "Toggle if players should be able to send a teleport request to you.");
    }
    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (TpaPlugin.get().manager.toggleTpa(playerRef.getUuid())) {
            context.sendMessage(Message.raw("Players can no longer request to teleport to you."));
        } else {
            context.sendMessage(Message.raw("Players can now request to teleport to you."));
        }
    }
}
