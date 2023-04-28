package bluesea.aquautils

import bluesea.aquautils.common.CommonAudience
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

open class PluginAudience protected constructor(audience: Audience, source: CommandSource) : CommonAudience<CommandSource>(audience, source) {
    override fun isPlayer(): Boolean {
        return source is Player
    }

    override fun hasPermission(permission: String): Boolean {
        return source.hasPermission(permission)
    }

    override fun sendMessage(component: Component) {
        source.sendMessage(component)
    }
}
