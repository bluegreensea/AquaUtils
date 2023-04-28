package bluesea.aquautils

import bluesea.aquautils.common.LegacyController
import com.james090500.VelocityGUI.VelocityGUI
import com.james090500.VelocityGUI.helpers.InventoryLauncher
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.scheduler.ScheduledTask
import java.util.UUID
import java.util.concurrent.TimeUnit
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

class PluginListener {
    val lobbyPlayer = HashMap<UUID, Int>()
    private val scheduledTasks = HashMap<UUID, ScheduledTask>()

    @Subscribe
    fun onPlayerChat(event: PlayerChatEvent) {
        val player = Audience.audience(event.player)
        if (LegacyController.onPlayerMessage(player, event.message)) {
            if (!event.player.hasPermission("aquautils.kick.bypass")) {
                event.player.disconnect(Component.text("已重複3次(含)以上"))
            }
        }
    }

    @Subscribe
    fun onPlayerJoin(event: ServerPostConnectEvent) {
        val plugin = AquaUtilsVelocity.getInstance()
        val player = event.player
        if (player.currentServer.get().serverInfo.name == "lobby") {
            lobbyPlayer[player.uniqueId] = 1
            scheduledTasks[player.uniqueId] = plugin.getServer().scheduler
                .buildTask(plugin) { it: ScheduledTask ->
                    if (player.currentServer.get().serverInfo.name == "lobby") {
                        InventoryLauncher(VelocityGUI.instance).execute("servers", player)
                        lobbyPlayer[player.uniqueId] = lobbyPlayer[player.uniqueId]!! + 1
                        if (lobbyPlayer[player.uniqueId]!! > 2) {
                            scheduledTasks.remove(player.uniqueId)
                            it.cancel()
                        }
                    } else {
                        scheduledTasks.remove(player.uniqueId)
                        it.cancel()
                    }
                }
                .repeat(3L, TimeUnit.SECONDS)
                .schedule()
        } else {
            lobbyPlayer.remove(player.uniqueId)
        }
    }

    @Subscribe
    fun onPlayerLeave(event: DisconnectEvent) {
        val player = event.player
        lobbyPlayer.remove(player.uniqueId)
        if (scheduledTasks[player.uniqueId] != null) {
            scheduledTasks[player.uniqueId]!!.cancel()
            scheduledTasks.remove(player.uniqueId)
        }
    }
}
