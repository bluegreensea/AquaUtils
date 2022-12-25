package bluesea.aquautils;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "aquautils", name = "AquaUtils", version = "${version}", authors = "bluegreensea")
public class AquaUtilsVelocity {
    private static ProxyServer server;
    private static Logger logger;

    @Inject
    public AquaUtilsVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        CommandManager commandManager = server.getCommandManager();
        PluginCommand command = new PluginCommand();
        commandManager.register("aquautils", command, "au");
        commandManager.register("votereset", command);
        commandManager.register("voteget", command);
        commandManager.register("vkick", command);

        server.getEventManager().register(this, new PluginListener());
    }

    public static ProxyServer getServer() {
        return server;
    }

    public static Logger getLogger() {
        return logger;
    }
}
