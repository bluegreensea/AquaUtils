package bluesea.aquautils

import bluesea.aquautils.common.Constants
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type
import net.minecraft.resources.ResourceLocation

@JvmRecord
data class VoteDataPacket(val bytes: ByteArray) : CustomPacketPayload {
    companion object {
        val ID: Type<VoteDataPacket> = Type(ResourceLocation.fromNamespaceAndPath(Constants.ID, "test"))
        val CODEC: StreamCodec<FriendlyByteBuf, VoteDataPacket> = CustomPacketPayload.codec(
            VoteDataPacket::write,
            VoteDataPacket::read
        )

        fun read(buf: FriendlyByteBuf): VoteDataPacket {
            val bytes = buf.readBytes(buf.readableBytes()).array()
            return VoteDataPacket(bytes)
        }
    }

    fun write(buf: FriendlyByteBuf) {
        buf.writeByteArray(bytes)
    }

    override fun type(): Type<out CustomPacketPayload> = ID
}
