package bluesea.aquautils.util

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection
import com.velocitypowered.proxy.connection.client.ConnectedPlayer
import dev.simplix.protocolize.api.PacketDirection
import dev.simplix.protocolize.api.Protocol
import dev.simplix.protocolize.api.Protocolize
import dev.simplix.protocolize.api.packet.AbstractPacket
import dev.simplix.protocolize.velocity.packet.VelocityProtocolizePacket
import dev.simplix.protocolize.velocity.player.VelocityProtocolizePlayer
import java.util.UUID

class VelocityProtocolizePlayer(private val player: Player) : VelocityProtocolizePlayer(null, null) {
    companion object {
        private val REGISTRATION_PROVIDER = Protocolize.protocolRegistration()
    }

    override fun sendPacket(packet: Any) {
        var finalPacket = packet
        if (packet is AbstractPacket) {
            val pack = REGISTRATION_PROVIDER.createPacket(
                packet.javaClass as Class<out AbstractPacket>,
                Protocol.PLAY,
                PacketDirection.CLIENTBOUND,
                protocolVersion()
            ) as VelocityProtocolizePacket?
                ?: throw IllegalStateException("Cannot send " + packet.javaClass.name + " to players with protocol version " + protocolVersion())
            pack.wrapper(packet)
            finalPacket = pack
        }
        (player as ConnectedPlayer).connection.write(finalPacket)
    }

    override fun sendPacketToServer(packet: Any) {
        val finalPacket = if (packet is AbstractPacket) {
            val pack = REGISTRATION_PROVIDER.createPacket(
                packet.javaClass as Class<out AbstractPacket>,
                Protocol.PLAY,
                PacketDirection.SERVERBOUND,
                protocolVersion()
            ) as VelocityProtocolizePacket?
                ?: throw IllegalStateException("Cannot send " + packet.javaClass.name + " to players with protocol version " + protocolVersion())
            pack.wrapper(packet)
            pack
        } else {
            packet
        }
        player.currentServer
            .ifPresent { serverConnection: ServerConnection ->
                (serverConnection as VelocityServerConnection).connection!!.write(finalPacket)
            }
    }

    override fun protocolVersion(): Int {
        return player.protocolVersion.protocol
    }

    override fun <T> handle(): T {
        return player as T
    }

    override fun uniqueId(): UUID {
        return player.uniqueId
    }
}
