package bluesea.aquautils

import bluesea.aquautils.common.Constants
import bluesea.aquautils.common.Controller
import com.james090500.VelocityGUI.VelocityGUI
import com.james090500.VelocityGUI.helpers.InventoryLauncher
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import java.util.UUID
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

class PluginListener(private val plugin: AquaUtilsVelocity) {
    private val lobbyPlayer = HashMap<UUID, Int>()

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
        val player = event.player
        if (plugin.velocityGUI != null && plugin.velocityGUI is VelocityGUI) {
            player.currentServer.ifPresent { currentServer ->
                if (currentServer.serverInfo.name == "minigame") {
                    try {
                        InventoryLauncher(plugin.velocityGUI).execute("servers", player)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    @Subscribe
    fun onPlayerDisconnect(event: DisconnectEvent) {
        val player = event.player
        lobbyPlayer.remove(player.uniqueId)
    }
}
