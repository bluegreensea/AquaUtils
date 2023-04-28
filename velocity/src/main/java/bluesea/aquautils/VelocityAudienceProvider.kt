package bluesea.aquautils

import bluesea.aquautils.common.CommonAudienceProvider
import com.velocitypowered.api.command.CommandSource
import net.kyori.adventure.text.Component

class VelocityAudienceProvider(plugin: AquaUtilsVelocity) : CommonAudienceProvider<CommandSource> {
    private val plugin: AquaUtilsVelocity
    private val consoleServerAudience: VelocityAudience

    init {
        this.plugin = plugin
        consoleServerAudience = VelocityAudience(
            plugin.getServer().consoleCommandSource,
            plugin.getServer().consoleCommandSource
        )
    }

    override fun getConsoleServerAudience(): VelocityAudience {
        return consoleServerAudience
    }

    override operator fun get(source: CommandSource): VelocityAudience {
        return VelocityAudience(source, source)
    }

    override fun broadcast(component: Component, permission: String) {
        for (player in plugin.getServer().allPlayers) {
            if (player.hasPermission(permission)) {
                player.sendMessage(component)
            }
        }
    }
}
