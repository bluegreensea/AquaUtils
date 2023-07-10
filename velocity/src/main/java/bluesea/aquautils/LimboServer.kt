package bluesea.aquautils

import com.velocitypowered.api.event.Subscribe
import java.util.LinkedList
import net.elytrium.limboapi.api.Limbo
import net.elytrium.limboapi.api.chunk.VirtualWorld
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent
import net.elytrium.limboapi.api.player.LimboPlayer

class LimboServer(private val plugin: AquaUtilsVelocity) {
    val players = LinkedList<LimboPlayer>()
    lateinit var world: VirtualWorld
    lateinit var server: Limbo

    @Subscribe
    fun onLoginLimboRegisterEvent(event: LoginLimboRegisterEvent) {
        val player = event.player
        event.addOnJoinCallback {
            server.spawnPlayer(player, LimboHandler(plugin))
        }
    }
}
