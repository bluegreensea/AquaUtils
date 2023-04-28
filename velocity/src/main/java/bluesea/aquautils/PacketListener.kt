package bluesea.aquautils

import dev.simplix.protocolize.api.Direction
import dev.simplix.protocolize.api.listener.AbstractPacketListener
import dev.simplix.protocolize.api.listener.PacketReceiveEvent
import dev.simplix.protocolize.api.listener.PacketSendEvent
import dev.simplix.protocolize.data.packets.HeldItemChange
import dev.simplix.protocolize.data.packets.UseItem

class PacketListener {
    class UseItemPacketListener : AbstractPacketListener<UseItem>(UseItem::class.java, Direction.UPSTREAM, 0) {
        override fun packetReceive(event: PacketReceiveEvent<UseItem>) {
        }

        override fun packetSend(event: PacketSendEvent<UseItem>) {
        }
    }

    class HeldItemChangePacketListener : AbstractPacketListener<HeldItemChange>(HeldItemChange::class.java, Direction.DOWNSTREAM, 0) {
        override fun packetReceive(event: PacketReceiveEvent<HeldItemChange>) {
        }

        override fun packetSend(event: PacketSendEvent<HeldItemChange>) {
        }
    }
}
