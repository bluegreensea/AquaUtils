package bluesea.aquautils

import bluesea.aquautils.common.Controller
import com.google.inject.Inject
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.audience.Audience
import org.slf4j.Logger

@Plugin(
    id = "aquautils",
    name = "AquaUtils",
    version = "\${version}",
    authors = ["bluegreensea"],
    dependencies = [
        Dependency(id = "mckotlin-velocity")
    ]
)
class AquaUtilsVelocity @Inject constructor(server: ProxyServer, logger: Logger) {
    companion object {
        lateinit var server: ProxyServer
            private set
        lateinit var logger: Logger
            private set

        var ytChatLooper: Thread? = null
    }

    init {
        Companion.server = server
        Companion.logger = logger
    }

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        val commandManager = server.commandManager

        val aquautilsNode = Controller.aquautils()
        val aquautilsCommand = BrigadierCommand(aquautilsNode)
        commandManager.register(aquautilsCommand)

        val voteresetNode = Controller.votereset(Audience.audience(server))
        val voteresetCommand = BrigadierCommand(voteresetNode)
        commandManager.register(voteresetCommand)

        val votegetNode = Controller.voteget(Audience.audience(server))
        val votegetCommand = BrigadierCommand(votegetNode)
        commandManager.register(votegetCommand)

        val votesetNode = Controller.voteset(Audience.audience(server))
        val votesetCommand = BrigadierCommand(votesetNode)
        commandManager.register(votesetCommand)

        commandManager.register(PluginCommand.vkickCreateBrigadierCommand(server))
        commandManager.register(PluginCommand.vgetbrandCreateBrigadierCommand(server))
        commandManager.register(PluginCommand.vytchat(server))

        server.eventManager.register(this, PluginListener())
    }

    @Subscribe
    fun onProxyShutdownEvent(event: ProxyShutdownEvent) {
        if (ytChatLooper != null && ytChatLooper!!.isAlive) {
            ytChatLooper!!.interrupt()
            logger.info("YTChat 已關閉!")
        }
    }
}
