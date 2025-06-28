package bluesea.aquautils

import bluesea.aquautils.common.CommonAudienceProvider
import bluesea.aquautils.common.CommonParsersProvider
import bluesea.aquautils.common.parser.CommonPlayers
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.MinecraftServer

class FabricAudienceProvider(private val server: MinecraftServer) : CommonAudienceProvider<CommandSourceStack, FabricAudience> {
    override fun parsersProvider(): CommonParsersProvider<CommandSourceStack, FabricAudience, *, *> {
        TODO("Not yet implemented")
    }

    override fun getConsoleServerAudience(): FabricAudience {
        return null!!
    }

    override fun getAllPlayers(): CommonPlayers<FabricAudience> {
        return CommonPlayers(server.playerList.players.map { FabricAudience(it.createCommandSourceStack(), it as Audience) })
    }

    override fun get(source: CommandSourceStack): FabricAudience {
        return FabricAudience(source, source as Audience)
    }

    override fun broadcast(source: CommandSourceStack, component: Component, permission: String) {
        for (player in server.playerList.players) {
            if (player.hasPermissions(4)) {
                (player as Audience).sendMessage(component)
            }
        }
    }
}
