package bluesea.aquautils

import bluesea.aquautils.common.Constants
import bluesea.aquautils.common.Controller
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

class PluginListener(private val plugin: AquaUtilsVelocity) {
    private val lobbyPlayer = HashMap<UUID, Int>()
    private val scheduledTasks = HashMap<UUID, ScheduledTask>()

    @Subscribe
    fun onPlayerChat(event: PlayerChatEvent) {
        val player = Audience.audience(event.player)
        val kick = Controller.onPlayerMessage(
            player,
            event.message,
            VelocityAudience(plugin.proxyServer, plugin.proxyServer.consoleCommandSource)
        )
        if (kick) {
            if (!event.player.hasPermission("${Constants.MOD_ID}.kick.bypass")) {
                event.player.disconnect(Component.text("已重複3次(含)以上"))
            }
        }
    }

    @Suppress("UnstableApiUsage")
    @Subscribe
    fun onPlayerServerConnect(event: ServerPostConnectEvent) {
        if (plugin.velocityGUI != null && plugin.velocityGUI is VelocityGUI) {
            val player = event.player
            if (player.currentServer.get().serverInfo.name == "minigame") {
                lobbyPlayer[player.uniqueId] = 1
                scheduledTasks[player.uniqueId] = plugin.proxyServer.scheduler
                    .buildTask(plugin) { it: ScheduledTask ->
                        try {
                            if (player.currentServer.isPresent && player.currentServer.get().serverInfo.name == "minigame") {
                                InventoryLauncher(plugin.velocityGUI).execute("servers", player)
                                lobbyPlayer[player.uniqueId] = lobbyPlayer[player.uniqueId]!! + 1
                                if (lobbyPlayer[player.uniqueId]!! > 2) {
                                    scheduledTasks.remove(player.uniqueId)
                                    it.cancel()
                                }
                            } else {
                                scheduledTasks.remove(player.uniqueId)
                                it.cancel()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
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
    }

    @Subscribe
    fun onPlayerDisconnect(event: DisconnectEvent) {
        val player = event.player
        lobbyPlayer.remove(player.uniqueId)
        if (scheduledTasks[player.uniqueId] != null) {
            scheduledTasks[player.uniqueId]!!.cancel()
            scheduledTasks.remove(player.uniqueId)
        }
    }
}
