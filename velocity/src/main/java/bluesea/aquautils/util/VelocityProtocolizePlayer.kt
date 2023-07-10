package bluesea.aquautils.util

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection
import com.velocitypowered.proxy.connection.client.ConnectedPlayer
import dev.simplix.protocolize.api.Location
import dev.simplix.protocolize.api.PacketDirection
import dev.simplix.protocolize.api.PlayerInteract
import dev.simplix.protocolize.api.Protocol
import dev.simplix.protocolize.api.Protocolize
import dev.simplix.protocolize.api.inventory.Inventory
import dev.simplix.protocolize.api.inventory.PlayerInventory
import dev.simplix.protocolize.api.packet.AbstractPacket
import dev.simplix.protocolize.api.player.ProtocolizePlayer
import dev.simplix.protocolize.velocity.packet.VelocityProtocolizePacket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class VelocityProtocolizePlayer(private val player: Player) : ProtocolizePlayer {
    private val interactConsumers: MutableList<Consumer<PlayerInteract>> = ArrayList()
    private val windowId = AtomicInteger(101)
    private val registeredInventories: MutableMap<Int, Inventory> = ConcurrentHashMap()
    private val proxyInventory = PlayerInventory(this)
    private val location = Location(0.0, 0.0, 0.0, 0f, 0f)
    override fun sendPacket(packet: Any) {
        var finalPacket = packet
        if (packet is AbstractPacket) {
            val pack = REGISTRATION_PROVIDER.createPacket(
                packet.javaClass as Class<out AbstractPacket>,
                Protocol.PLAY,
                PacketDirection.CLIENTBOUND,
                protocolVersion()
            ) as VelocityProtocolizePacket
                ?: throw IllegalStateException("Cannot send " + packet.javaClass.name + " to players with protocol version " + protocolVersion())
            pack.wrapper(packet)
            finalPacket = pack
        }
        (player as ConnectedPlayer).connection.write(finalPacket)
    }

    override fun sendPacketToServer(packet: Any) {
        var packet = packet
        if (packet is AbstractPacket) {
            val pack = REGISTRATION_PROVIDER.createPacket(
                packet.javaClass as Class<out AbstractPacket>,
                Protocol.PLAY,
                PacketDirection.SERVERBOUND,
                protocolVersion()
            ) as VelocityProtocolizePacket
                ?: throw IllegalStateException("Cannot send " + packet.javaClass.name + " to players with protocol version " + protocolVersion())
            pack.wrapper(packet)
            packet = pack
        }
        val finalPacket = packet
        player.currentServer
            .ifPresent { serverConnection: ServerConnection ->
                (serverConnection as VelocityServerConnection).connection!!
                    .write(finalPacket)
            }
    }

    override fun generateWindowId(): Int {
        var out = windowId.incrementAndGet()
        if (out >= 200) {
            out = 101
            windowId.set(101)
        }
        return out
    }

    override fun protocolVersion(): Int {
        return player.protocolVersion.protocol
    }

    override fun <T> handle(): T {
        return player as T
    }

    override fun onInteract(interactConsumer: Consumer<PlayerInteract>) {
        interactConsumers.add(interactConsumer)
    }

    override fun handleInteract(interact: PlayerInteract) {
        interactConsumers.forEach(
            Consumer { interactConsumer: Consumer<PlayerInteract> ->
                interactConsumer.accept(
                    interact
                )
            }
        )
    }

    companion object {
        private val REGISTRATION_PROVIDER = Protocolize.protocolRegistration()
    }

    override fun uniqueId(): UUID {
        return player.uniqueId
    }

    override fun proxyInventory(): PlayerInventory {
        return proxyInventory
    }

    override fun registeredInventories(): MutableMap<Int, Inventory> {
        return registeredInventories
    }

    override fun location(): Location {
        return location
    }
}
