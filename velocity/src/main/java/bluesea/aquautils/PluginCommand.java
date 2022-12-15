package bluesea.aquautils;

import bluesea.aquautils.common.Processor;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.util.List;
import net.kyori.adventure.audience.Audience;

public class PluginCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        if (invocation.source() instanceof Player) {
            Audience player = Audience.audience(invocation.source());
            Processor.onCommand(player, invocation.alias(), invocation.arguments());
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }
}
