package bluesea.aquautils

import bluesea.aquautils.common.Constants
import bluesea.aquautils.common.Controller
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator
import cloud.commandframework.velocity.VelocityCommandManager
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path
import kotlin.jvm.optionals.getOrNull
import net.elytrium.limboapi.api.LimboFactory
import net.elytrium.limboapi.api.chunk.Dimension
import org.slf4j.Logger

@Plugin(
    id = Constants.MOD_ID,
    name = Constants.MOD_NAME,
    version = Constants.MOD_VERSION,
    authors = ["bluegreensea"],
    dependencies = [
        Dependency(id = "mckotlin-velocity"),
        Dependency(id = "velocitygui", optional = true),
        Dependency(id = "limboapi", optional = true),
        Dependency(id = "protocolize", optional = true)
    ]
)
class AquaUtilsVelocity @Inject constructor(server: ProxyServer, pluginContainer: PluginContainer, logger: Logger, @DataDirectory dataDirectory: Path) {
    val proxyServer: ProxyServer
    private val logger: Logger
    private val dataDirectory: Path
    private val pluginContainer: PluginContainer
    private var pluginListener: PluginListener

    // private val config: FileConfig

    val velocityGUI: Any?
    val limboFactory: Any?

    lateinit var limbo: LimboServer

    init {
        this.proxyServer = server
        this.pluginContainer = pluginContainer
        this.logger = logger
        this.dataDirectory = dataDirectory

        // if (!dataDirectory.exists()) {
        //     dataDirectory.createDirectory()
        // }
        // val configFile = File("$dataDirectory${File.separator}config.toml")
        // if (!configFile.exists()) {
        //     configFile.createNewFile()
        // }
        // config = FileConfig.of(configFile)

        pluginListener = PluginListener(this)

        velocityGUI = proxyServer.pluginManager.getPlugin("velocitygui").flatMap(PluginContainer::getInstance).getOrNull()
        limboFactory = proxyServer.pluginManager.getPlugin("limboapi").flatMap(PluginContainer::getInstance).getOrNull()
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        // config.load()
        // Controller.kick = config.getOrElse("kick", Controller.kick)

        if (limboFactory != null && limboFactory is LimboFactory) {
            limbo = LimboServer(this)
            proxyServer.eventManager.register(this, limbo)
            limbo.world = limboFactory.createVirtualWorld(
                Dimension.valueOf("OVERWORLD"),
                0.0,
                100.0,
                0.0,
                90f,
                0.0f
            )
            limbo.server = limboFactory.createLimbo(limbo.world).setName("LimboLobby").setWorldTime(6000)
        }

        val audienceProvider = VelocityAudienceProvider(proxyServer)
        val veloctyCommandManager = VelocityCommandManager(
            pluginContainer,
            proxyServer,
            AsynchronousCommandExecutionCoordinator.builder<VelocityAudience>().build(),
            audienceProvider::get,
            VelocityAudience::source
        )

        Controller.register(veloctyCommandManager, audienceProvider)

        PluginCommand.register(veloctyCommandManager, proxyServer)

        proxyServer.eventManager.register(this, pluginListener)
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    fun onProxyShutdownEvent(event: ProxyShutdownEvent) {
        if (Controller.ytChatLooper != null && Controller.ytChatLooper!!.isAlive) {
            Controller.ytChatLooper!!.interrupt()
            logger.info("YTChat 已關閉!")
        }
        proxyServer.eventManager.unregisterListener(this, pluginListener)
        if (limboFactory != null && limboFactory is LimboFactory) {
            proxyServer.eventManager.unregisterListener(this, limbo)
        }
        // config.close()
    }
}
