package bluesea.aquautils

import bluesea.aquautils.common.LegacyController
import com.google.inject.Inject
import com.google.inject.name.Named
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path
import net.kyori.adventure.audience.Audience
import org.slf4j.Logger

@Plugin(
    id = "aquautils",
    name = "AquaUtils",
    version = "\${version}",
    authors = ["bluegreensea"],
    dependencies = [
        Dependency(id = "mckotlin-velocity"),
        Dependency(id = "velocitygui", optional = true),
        Dependency(id = "protocolize")
    ]
)
class AquaUtilsVelocity @Inject constructor(server: ProxyServer, logger: Logger, @DataDirectory dataDirectory: Path) {
    companion object {
        private lateinit var instace: AquaUtilsVelocity
        private lateinit var logger: Logger
        lateinit var pluginListener: PluginListener

        fun getInstance(): AquaUtilsVelocity {
            return instace
        }
    }

    private val server: ProxyServer
    private val dataDirectory: Path

    @Inject
    @Named("aquautils")
    private val pluginContainer: PluginContainer? = null

    init {
        instace = this
        this.server = server
        Companion.logger = logger
        this.dataDirectory = dataDirectory
        pluginListener = PluginListener()
    }

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        // val chatProvider = VelocityAudienceProvider(this)
        //
        // val commandmanager = VelocityCommandManager(
        //     this.getPluginContainer(),
        //     this.server,
        //     AsynchronousCommandExecutionCoordinator.builder<VelocityAudience>().build(),
        //     chatProvider::get,
        //     VelocityAudience::source
        // )
        //
        // CommandController.register<VelocityCommandManager<VelocityAudience>, VelocityAudience, CommandSource>(commandmanager, server)

        val commandManager = server.commandManager

        val aquautilsNode = LegacyController.aquautils()
        val aquautilsCommand = BrigadierCommand(aquautilsNode)
        commandManager.register(aquautilsCommand)

        val voteresetNode = LegacyController.votereset(Audience.audience(server))
        val voteresetCommand = BrigadierCommand(voteresetNode)
        commandManager.register(voteresetCommand)

        val votegetNode = LegacyController.voteget(Audience.audience(server))
        val votegetCommand = BrigadierCommand(votegetNode)
        commandManager.register(votegetCommand)

        val votesetNode = LegacyController.voteset(Audience.audience(server))
        val votesetCommand = BrigadierCommand(votesetNode)
        commandManager.register(votesetCommand)

        commandManager.register(PluginCommand.vkickCreateBrigadierCommand(server))
        commandManager.register(PluginCommand.vgetbrandCreateBrigadierCommand(server))
        commandManager.register(PluginCommand.vytchat(server))

        server.eventManager.register(this, pluginListener)
    }

    @Subscribe
    fun onProxyShutdownEvent(event: ProxyShutdownEvent) {
        if (LegacyController.ytChatLooper != null && LegacyController.ytChatLooper!!.isAlive) {
            LegacyController.ytChatLooper!!.interrupt()
            logger.info("YTChat 已關閉!")
        }
        server.eventManager.unregisterListener(this, pluginListener)
    }

    fun getServer(): ProxyServer {
        return server
    }
}
