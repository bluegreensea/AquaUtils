package bluesea.aquautils

import bluesea.aquautils.common.Controller
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.PlayerClientBrandEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import java.net.Inet4Address

internal class PluginListener {
    @Subscribe
    fun onPlayerChat(event: PlayerChatEvent) {
        val player = Audience.audience(event.player)
        if (Controller.onPlayerMessage(player, event.message)) {
            if (!event.player.hasPermission("aquautils.kick.bypass")) {
                event.player.disconnect(Component.text("已重複3次(含)以上"))
            }
        }
    }

    @Subscribe
    fun onPlayerClientBrand(event: PlayerClientBrandEvent) {
        AquaUtilsVelocity.logger.info(event.player.username + " joined with brand " + event.brand)
        if (event.brand.contains("Feather")) { // Feather fabric
            if (event.player.remoteAddress.address is Inet4Address) {
                event.player.disconnect(
                    Component.empty()
                        .append(Component.translatable("disconnect.closed"))
                        .append(Component.text("\n備註：\n偵測到Feather，此模組有問題導致此中斷\n請使用IPv6登入或不使用Feather"))
                )
            }
        }
    }
}
