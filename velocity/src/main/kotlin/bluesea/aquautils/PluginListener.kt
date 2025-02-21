package bluesea.aquautils

import bluesea.aquautils.common.Constants
import bluesea.aquautils.common.Controller
import com.james090500.VelocityGUI.VelocityGUI
import com.james090500.VelocityGUI.helpers.InventoryLauncher
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.PlayerClientBrandEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import java.util.UUID
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

class PluginListener(private val plugin: AquaUtilsVelocity) {
    private val lobbyPlayer = HashMap<UUID, Int>()

    @Subscribe
    fun onPlayerChat(event: PlayerChatEvent) {
        val player = Audience.audience(event.player)
        Controller.onPlayerMessage(
            player,
            event.message,
            VelocityAudience(plugin.proxyServer.consoleCommandSource, plugin.proxyServer)
        )
        val kick = Controller.onPlayerDetectSpam(player, event.message)
        if (kick) {
            if (!event.player.hasPermission("${Constants.ID}.kick.bypass")) {
                event.player.disconnect(Component.text("已重複3次(含)以上"))
            }
        }
    }

    @Subscribe
    fun onPlayerServerConnect(event: ServerPostConnectEvent) {
        val player = event.player
        VelocityAudience(player).setServerLinks(Controller.getServerLinks())
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

    @Subscribe
    fun onPlayerClientBrand(event: PlayerClientBrandEvent) {
        Controller.LOGGER.info(event.player.username + " joined with brand " + event.brand)
        if (event.brand.contains("Feather")) { // Feather fabric
            event.player.disconnect(
                Component.empty()
                    .append(Component.translatable("disconnect.closed"))
                    .append(Component.text("\n備註：\n偵測到 Feather，此模組有問題導致此中斷\n請不要使用 Feather"))
            )
        }
    }
}
