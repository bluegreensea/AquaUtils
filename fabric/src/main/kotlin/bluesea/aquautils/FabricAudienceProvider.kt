package bluesea.aquautils

import bluesea.aquautils.common.CommonAudienceProvider
import bluesea.aquautils.common.CommonParsersProvider
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minecraft.commands.CommandSourceStack

class FabricAudienceProvider : CommonAudienceProvider<CommandSourceStack, FabricAudience> {
    override fun parsersProvider(): CommonParsersProvider<CommandSourceStack, FabricAudience, *, *> {
        TODO("Not yet implemented")
    }

    override fun getConsoleServerAudience(): FabricAudience {
        return null!!
    }

    override fun getAllPlayersAudience(source: CommandSourceStack): FabricAudience {
        return FabricAudience(source, source.server.playerList.players as Audience)
    }

    override fun get(source: CommandSourceStack): FabricAudience {
        return FabricAudience(source, source as Audience)
    }

    override fun broadcast(source: CommandSourceStack, component: Component, permission: String) {
        for (player in source.server.playerList.players) {
            if (player.hasPermissions(4)) {
                (player as Audience).sendMessage(component)
            }
        }
    }

    override fun apply(source: CommandSourceStack): Audience {
        return source as Audience
    }
}
