package bluesea.aquautils

import bluesea.aquautils.common.CommonAudience
import io.netty.buffer.ByteBuf
import java.net.URI
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket
import net.minecraft.server.ServerLinks
import net.minecraft.server.level.ServerPlayer

class FabricAudience(source: CommandSourceStack, audience: Audience) : CommonAudience<CommandSourceStack>(source, audience) {
    override fun hasPermission(permission: String): Boolean {
        return source.hasPermission(4)
    }

    override fun setServerLinks(serverLinks: List<Pair<Component, String>>) {
        audience.forEachAudience { player ->
            if (player is ServerPlayer) {
                val newServerLinks = serverLinks.map { (component, string) ->
                    ServerLinks.Entry.custom(
                        component as net.minecraft.network.chat.Component,
                        URI.create(string)
                    )
                }
                val packet = ClientboundServerLinksPacket(ServerLinks(newServerLinks).untrust())
                player.connection.send(packet)
            }
        }
    }

    override fun sendPluginMessage(path: String, data: ByteBuf) {
        audience.forEachAudience { player ->
            if (player is ServerPlayer) {
                if (ServerPlayNetworking.canSend(player, VoteDataPacket.ID)) {
                    ServerPlayNetworking.send(
                        player,
                        VoteDataPacket(data.array())
                    )
                }
            }
        }
    }
}
