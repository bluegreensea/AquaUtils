package bluesea.aquautils

import bluesea.aquautils.common.Constants
import bluesea.aquautils.common.Controller
import bluesea.aquautils.parser.VelocityPlayers
import bluesea.aquautils.util.InventoryLauncher
import com.james090500.VelocityGUI.VelocityGUI
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.PlayerClientBrandEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.proxy.Player
import dev.simplix.protocolize.api.chat.ChatElement
import dev.simplix.protocolize.api.item.ItemStack
import dev.simplix.protocolize.data.ItemType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.util.Optional
import java.util.UUID
import net.kyori.adventure.text.Component

class PluginListener(private val plugin: AquaUtilsVelocity) {
    private val proxyServer = plugin.proxyServer
    private val lobbyPlayer = HashMap<UUID, Int>()

    @Subscribe
    fun onPlayerChat(event: PlayerChatEvent) {
        val player = event.player
        Controller.onPlayerMessage(
            VelocityAudience(player),
            event.message,
            Optional.of(VelocityAudience(proxyServer.consoleCommandSource)),
            VelocityPlayers.allPlayers(proxyServer)
        )
        val kick = Controller.onPlayerDetectSpam(player, event.message)
        if (kick) {
            if (!player.hasPermission("${Constants.ID}.kick.bypass")) {
                player.disconnect(Component.text("已重複3次(含)以上"))
            }
        }
    }

    @Subscribe
    fun onPlayerLogin(event: PostLoginEvent) {
        val player = event.player
        Controller.onPlayerLogin(
            player,
            Optional.of(VelocityAudience(proxyServer.consoleCommandSource)),
            VelocityPlayers.allPlayers(proxyServer)
        )
        VelocityAudience(player).setServerLinks(Controller.getServerLinks())
    }

    private fun openInventory(velocityGUI: VelocityGUI, player: Player) {
        InventoryLauncher(velocityGUI).execute(Controller.serversPanel, player) { panel, inventoryBuilder ->
            val address = player.remoteAddress.address
            var ip: InetSocketAddress? = null
            var alreadyConnected = true
            try {
                if (Controller.fallbackServerIp.isNotEmpty()) {
                    val ipAddress = InetAddress.getByName(Controller.fallbackServerIp)
                    ip = InetSocketAddress(ipAddress.hostAddress, Controller.fallbackServerPort)
                } else {
                    val url = URI(Controller.serverIpService)
                    val urlConnection = url.toURL().openConnection() as HttpURLConnection
                    if (urlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader(InputStreamReader(urlConnection.inputStream)).use { br ->
                            ip = InetSocketAddress(br.readLine(), Controller.fallbackServerPort)
                        }
                    }
                }
                ip?.let {
                    alreadyConnected = if (player.virtualHost.get().address != null && it.address != null) {
                        player.virtualHost.get().address.hostAddress == it.address.hostAddress
                    } else {
                        player.virtualHost.get().hostName == it.hostName
                    }
                }
            } catch (e: Exception) {
                Controller.LOGGER.error("", e)
            }
            val slot = 9 * (panel.rows - 1) + 8
            if (address is Inet4Address && !(address.isAnyLocalAddress || alreadyConnected)) {
                val itemStack = ItemStack(ItemType.RED_WOOL)
                itemStack.displayName(ChatElement.of(Component.text("切換連線")))
                inventoryBuilder.items[slot] = itemStack
                return@execute Pair(slot) {
                    ip?.let {
                        player.transferToHost(it)
                    }
                }
            }
            val itemStack = ItemStack(ItemType.LIME_WOOL)
            itemStack.displayName(ChatElement.of(Component.text("已使用連線")))
            inventoryBuilder.items[slot] = itemStack
            return@execute Pair(-1) {}
        }
    }

    @Subscribe
    fun onPlayerServerConnect(event: ServerPostConnectEvent) {
        val player = event.player
        if (plugin.velocityGUI != null && plugin.velocityGUI is VelocityGUI) {
            player.currentServer.ifPresent { currentServer ->
                if (currentServer.serverInfo.name == Controller.lobbyServer) {
                    lobbyPlayer.merge(player.uniqueId, 1, Int::plus)
                    try {
                        openInventory(plugin.velocityGUI, player)
                    } catch (e: Exception) {
                        Controller.LOGGER.error("", e)
                    }
                }
            }
        }
    }

    @Subscribe
    fun onPlayerDisconnect(event: DisconnectEvent) {
        val player = event.player
        Controller.onPlayerDisconnect(
            player,
            Optional.of(VelocityAudience(proxyServer.consoleCommandSource)),
            VelocityPlayers.allPlayers(proxyServer)
        )
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
