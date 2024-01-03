package bluesea.aquautils

import bluesea.aquautils.common.CommonAudienceProvider
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.minecraft.commands.CommandSourceStack

class FabricAudienceProvider : CommonAudienceProvider<CommandSourceStack> {
    override fun getConsoleServerAudience(): FabricAudience {
        return null!!
    }

    override fun getAllPlayersAudience(source: CommandSourceStack): FabricAudience {
        return FabricAudience(Audience.audience(source.server.playerList.players), source)
    }

    override fun get(source: CommandSourceStack): FabricAudience {
        return FabricAudience(source, source)
    }

    override fun broadcast(source: CommandSourceStack, component: Component, permission: String) {
        for (player in source.server.playerList.players) {
            if (player.hasPermissions(4)) {
                val textComponent = component as TextComponent
                source.sendSystemMessage(net.minecraft.network.chat.Component.nullToEmpty(textComponent.content()))
            }
        }
    }
}
