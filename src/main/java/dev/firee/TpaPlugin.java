package dev.firee;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import dev.firee.commands.TpaCommand;
import dev.firee.commands.TpaToggleCommand;
import dev.firee.commands.TpacceptCommand;
import dev.firee.commands.TpcancelCommand;
import dev.firee.commands.TpdenyCommand;

import javax.annotation.Nonnull;


public class TpaPlugin extends JavaPlugin {
    private static TpaPlugin instance;
    public Manager manager;

    public static TpaPlugin get() {
        return instance;
    }

    public TpaPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }
    @Override
    protected void setup() {
        super.setup();
        getCommandRegistry().registerCommand(new TpaCommand());
        getCommandRegistry().registerCommand(new TpacceptCommand());
        getCommandRegistry().registerCommand(new TpdenyCommand());
        getCommandRegistry().registerCommand(new TpcancelCommand());
        getCommandRegistry().registerCommand(new TpaToggleCommand());
    }

    @Override
    protected void start() {
        manager = new Manager(this);
    }

    @Override
    protected void shutdown() {
        getLogger().atInfo().log("Plugin shutting down!");
        if (manager != null) {
            if (manager.save("config.json")) {
                getLogger().atInfo().log("Saved config.json");
            } else {
                getLogger().atWarning().log("Couldn't save config.json");
            }
            getTaskRegistry().shutdown();
        }
        instance = null;
    }

}
