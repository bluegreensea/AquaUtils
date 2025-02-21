package bluesea.aquautils

import bluesea.aquautils.common.CommonAudience
import bluesea.aquautils.common.Constants
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.util.ServerLink
import io.netty.buffer.ByteBuf
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

class VelocityAudience(source: CommandSource, audience: Audience) : CommonAudience<CommandSource>(source, audience) {
    constructor(source: CommandSource) : this(source, source)

    override fun hasPermission(permission: String): Boolean {
        return source.hasPermission(permission)
    }

    override fun sendMessage(component: Component) {
        audience.sendMessage(component)
    }

    override fun setServerLinks(serverLinks: List<Pair<Component, String>>) {
        audience.forEachAudience { player ->
            if (player is Player) {
                val newServerLinks = serverLinks.map { (component, string) ->
                    ServerLink.serverLink(component, string)
                }
                try {
                    player.setServerLinks(newServerLinks)
                } catch (_: IllegalArgumentException) {}
            }
        }
    }

    override fun sendPluginMessage(path: String, data: ByteBuf) {
        audience.forEachAudience { player ->
            if (player is Player) {
                player.sendPluginMessage(
                    MinecraftChannelIdentifier.create(Constants.ID, path),
                    data.array()
                )
            }
        }
    }
}
