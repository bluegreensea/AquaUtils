package bluesea.aquautils;

import bluesea.aquautils.common.Controller;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public class PluginCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        if (invocation.source() instanceof Player) {
            Audience players = Audience.audience(AquaUtilsVelocity.getServer().getAllPlayers());
            Audience player = invocation.source();

            if (invocation.alias().equals("vkick")) {
                if (invocation.arguments().length > 0) {
                    for (Player kickPlayer : AquaUtilsVelocity.getServer().getAllPlayers()) {
                        if (kickPlayer.getUsername().equals(invocation.arguments()[0])) {
                            kickPlayer.disconnect(
                                Component.translatable("multiplayer.disconnect.kicked"));
                            break;
                        }
                    }
                }
                return;
            }
            Controller.onCommand(players, player, invocation.alias(), invocation.arguments());
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.alias().equals("vkick") && invocation.arguments().length <= 1) {
            return AquaUtilsVelocity.getServer().getAllPlayers().stream().map(Player::getUsername).collect(Collectors.toList());
        }
        return SimpleCommand.super.suggest(invocation);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("aquautils." + invocation.alias());
    }
}
