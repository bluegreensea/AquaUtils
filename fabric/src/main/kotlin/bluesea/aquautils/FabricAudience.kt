package bluesea.aquautils

import bluesea.aquautils.common.CommonAudience
import bluesea.aquautils.common.Constants
import io.netty.buffer.ByteBuf
import java.net.URI
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.ServerLinks
import net.minecraft.server.level.ServerPlayer

class FabricAudience(audience: Audience, source: CommandSourceStack) : CommonAudience<CommandSourceStack>(audience, source) {
    override val isPlayer = source.isPlayer

    override fun hasPermission(permission: String): Boolean {
        return source.hasPermission(4)
    }

    override fun sendMessage(component: Component) {
        val textComponent = component as TextComponent
        source.sendSystemMessage(net.minecraft.network.chat.Component.nullToEmpty(textComponent.content()))
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
        ServerPlayNetworking.send(
            source.player,
            ResourceLocation.tryBuild(Constants.MOD_ID, path),
            FriendlyByteBuf(data)
        )
    }
}
