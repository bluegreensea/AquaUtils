package bluesea.aquautils

import bluesea.aquautils.common.Controller
import bluesea.aquautils.util.InventoryLauncher
import com.james090500.VelocityGUI.VelocityGUI
import java.util.concurrent.TimeUnit
import net.elytrium.limboapi.api.Limbo
import net.elytrium.limboapi.api.LimboSessionHandler
import net.elytrium.limboapi.api.player.LimboPlayer

class LimboHandler(private val plugin: AquaUtilsVelocity) : LimboSessionHandler {
    private lateinit var player: LimboPlayer

    override fun onSpawn(server: Limbo, player: LimboPlayer) {
        this.player = player
        this.player.disableFalling()
        plugin.limbo.players.add(player)

        plugin.proxyServer.scheduler
            .buildTask(plugin) { _ ->
                InventoryLauncher(plugin.velocityGUI as VelocityGUI)
                    .execute(Controller.serversPanel, player.proxyPlayer)
            }
            .repeat(5L, TimeUnit.SECONDS)
            .schedule()
    }

    override fun onDisconnect() {
        plugin.limbo.players.remove(this.player)
    }
}
