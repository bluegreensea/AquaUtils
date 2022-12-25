package bluesea.aquautils;

import bluesea.aquautils.common.Controller;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

class PluginListener {
    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Audience player = Audience.audience(event.getPlayer());

        if (Controller.onPlayerMessage(player, event.getMessage())) {
            event.getPlayer().disconnect(Component.text("已重複3次(含)以上"));
        }
    }
}
