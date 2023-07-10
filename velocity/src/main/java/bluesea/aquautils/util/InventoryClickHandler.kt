package bluesea.aquautils.util

import com.james090500.VelocityGUI.VelocityGUI
import com.james090500.VelocityGUI.helpers.InventoryLauncher
import com.velocitypowered.api.proxy.Player
import dev.simplix.protocolize.api.inventory.InventoryClick

class InventoryClickHandler(private val velocityGUI: VelocityGUI) {
    fun execute(commands: Array<String>, click: InventoryClick, player: Player) {
        for (command in commands) {
            val splitCommand = command.split("= ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            when (splitCommand[0]) {
                "open" -> InventoryLauncher(velocityGUI).execute(splitCommand[1], player)
                "close" -> click.player().closeInventory()
                "sudo" -> player.spoofChatInput(splitCommand[1])
                "vsudo" -> velocityGUI.server.commandManager.executeAsync(player, splitCommand[1])
                "server" -> player.createConnectionRequest(velocityGUI.server.getServer(splitCommand[1]).get()).connect()
            }
        }
    }
}
