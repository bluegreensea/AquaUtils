package bluesea.aquautils

import bluesea.aquautils.common.CommonAudience
import bluesea.aquautils.common.Constants
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import io.netty.buffer.ByteBuf
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

class VelocityAudience(audience: Audience, source: CommandSource) : CommonAudience<CommandSource>(audience, source) {
    override val isPlayer: Boolean = source is Player

    override fun hasPermission(permission: String): Boolean {
        return source.hasPermission(permission)
    }

    override fun sendMessage(component: Component) {
        source.sendMessage(component)
    }

    override fun sendPluginMessage(path: String, data: ByteBuf) {
        if (isPlayer) {
            val player = source as Player
            player.sendPluginMessage(
                MinecraftChannelIdentifier.create(Constants.MOD_ID, path),
                data.array()
            )
        }
    }
}
