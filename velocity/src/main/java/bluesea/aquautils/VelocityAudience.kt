package bluesea.aquautils

import bluesea.aquautils.common.CommonAudienceProvider
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

class VelocityAudience(audience: Audience, source: CommandSource) :
    PluginAudience(audience, source),
    CommonAudienceProvider<CommandSource> {
    override fun isPlayer(): Boolean {
        return source is Player
    }

    override fun hasPermission(permission: String): Boolean {
        return source.hasPermission(permission)
    }

    override fun sendMessage(component: Component) {
        source.sendMessage(component)
    }

    override fun getConsoleServerAudience(): CommonAudienceProvider<CommandSource> {
        TODO("Not yet implemented")
    }

    override fun broadcast(component: Component, permission: String) {
        TODO("Not yet implemented")
    }

    override fun get(source: CommandSource): CommonAudienceProvider<CommandSource> {
        TODO("Not yet implemented")
    }
}
