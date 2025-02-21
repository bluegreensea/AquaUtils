package bluesea.aquautils

import bluesea.aquautils.common.CommonAudienceProvider
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

class VelocityAudienceProvider(private val proxyServer: ProxyServer) : CommonAudienceProvider<CommandSource, VelocityAudience> {
    private val consoleServerAudience = VelocityAudience(
        proxyServer.consoleCommandSource,
        proxyServer.consoleCommandSource
    )

    override fun parsersProvider(): VelocityParsersProvider {
        return VelocityParsersProvider()
    }

    override fun getConsoleServerAudience(): VelocityAudience {
        return consoleServerAudience
    }

    override fun getAllPlayersAudience(source: CommandSource): VelocityAudience {
        return VelocityAudience(source, proxyServer)
    }

    override fun get(source: CommandSource): VelocityAudience {
        return VelocityAudience(source, source)
    }

    override fun broadcast(source: CommandSource, component: Component, permission: String) {
        for (player in proxyServer.allPlayers) {
            if (player.hasPermission(permission)) {
                player.sendMessage(component)
            }
        }
    }

    override fun apply(source: CommandSource): Audience {
        return source
    }
}
