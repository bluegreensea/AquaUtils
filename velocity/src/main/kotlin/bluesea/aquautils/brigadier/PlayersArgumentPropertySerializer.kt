package bluesea.aquautils.brigadier

import bluesea.aquautils.arguments.PlayersArgumentType
import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentPropertySerializer
import io.netty.buffer.ByteBuf

class PlayersArgumentPropertySerializer : ArgumentPropertySerializer<PlayersArgumentType> {
    companion object {
        val PLAYERS = PlayersArgumentPropertySerializer()
    }

    override fun serialize(`object`: PlayersArgumentType, buf: ByteBuf, protocolVersion: ProtocolVersion) {
        buf.writeByte(`object`.flags)
    }

    override fun deserialize(buf: ByteBuf, protocolVersion: ProtocolVersion): PlayersArgumentType {
        val byte = buf.readByte()
        return PlayersArgumentType(byte.toInt())
    }
}
