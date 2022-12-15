package bluesea.aquautils;

import bluesea.aquautils.common.Processor;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

class PluginListener {
    private final Logger log = AquaUtilsVelocity.getLogger();

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Audience player = Audience.audience(event.getPlayer());

        if (Processor.onPlayerMessage(player, event.getMessage())) {
            player.sendMessage(Component.text("已重複3次(含)以上"));
        }
    }
}
