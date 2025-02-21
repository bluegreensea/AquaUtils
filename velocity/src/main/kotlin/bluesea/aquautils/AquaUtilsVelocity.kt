package bluesea.aquautils

import bluesea.aquautils.brigadier.VelocityArgumentTypeRegistry
import bluesea.aquautils.brigadier.VelocityBrigadierMapper
import bluesea.aquautils.common.Constants
import bluesea.aquautils.common.Controller
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
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.velocity.VelocityCommandManager
import org.slf4j.Logger

@Plugin(
    id = Constants.ID,
    name = Constants.NAME,
    version = Constants.VERSION,
    authors = ["bluegreensea"],
    dependencies = [
        Dependency(id = "mckotlin-velocity"),
        Dependency(id = "velocitygui", optional = true),
        Dependency(id = "limboapi", optional = true),
        Dependency(id = "protocolize", optional = true)
    ]
)
class AquaUtilsVelocity @Inject constructor(
    val proxyServer: ProxyServer,
    private val pluginContainer: PluginContainer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path
) {
    private val pluginListener = PluginListener(this)

    // private val config: FileConfig

    val velocityGUI = proxyServer.pluginManager.getPlugin("velocitygui").flatMap(PluginContainer::getInstance).getOrNull()
    val limboFactory = proxyServer.pluginManager.getPlugin("limboapi").flatMap(PluginContainer::getInstance).getOrNull()

    lateinit var limbo: LimboServer

    init {
        // if (!dataDirectory.exists()) {
        //     dataDirectory.createDirectory()
        // }
        // val configFile = Path("$dataDirectory${File.separator}config.toml")
        // if (!configFile.exists()) {
        //     configFile.createFile()
        // }
        // config = FileConfig.of(configFile)
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        // config.load()
        // Controller.kick = config.getOrElse("kick", Controller.kick)

        val audienceProvider = VelocityAudienceProvider(proxyServer)
        val velocityCommandManager = VelocityCommandManager(
            pluginContainer,
            proxyServer,
            ExecutionCoordinator.builder<VelocityAudience>().synchronizeExecution().build(),
            SenderMapper.create(audienceProvider::get, VelocityAudience::source)
        )

        VelocityArgumentTypeRegistry.replace(logger)
        val velocityBrigadierManager = VelocityBrigadierMapper(velocityCommandManager)
        velocityBrigadierManager.registerMappings()

        Controller.register(velocityCommandManager, audienceProvider)

        PluginCommand.register(velocityCommandManager)

        proxyServer.eventManager.register(this, pluginListener)

        // other plugin
        if (limboFactory != null && limboFactory is LimboFactory) {
            limbo = LimboServer(this)
            proxyServer.eventManager.register(this, limbo)
            limbo.world = limboFactory.createVirtualWorld(
                Dimension.OVERWORLD,
                0.0,
                100.0,
                0.0,
                90f,
                0.0f
            )
            limbo.server = limboFactory.createLimbo(limbo.world).setName("LimboLobby").setWorldTime(6000)
            logger.info("enabled limboAPI support")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    fun onProxyShutdownEvent(event: ProxyShutdownEvent) {
        if (Controller.ytChatLooper != null && Controller.ytChatLooper!!.isAlive) {
            Controller.ytChatLooper!!.interrupt()
        }
        proxyServer.eventManager.unregisterListener(this, pluginListener)
        if (limboFactory != null && limboFactory is LimboFactory) {
            proxyServer.eventManager.unregisterListener(this, limbo)
        }

        VelocityArgumentTypeRegistry.restore(logger)

        // config.close()
    }
}
