package bluesea.aquautils.util

import com.james090500.VelocityGUI.VelocityGUI
import com.james090500.VelocityGUI.config.Configs
import com.james090500.VelocityGUI.helpers.InventoryBuilder
import com.velocitypowered.api.proxy.Player
import dev.simplix.protocolize.api.inventory.InventoryClick

class InventoryLauncher(private val velocityGUI: VelocityGUI) {
    fun execute(panelName: String, player: Player) {
        val protocolizePlayer = VelocityProtocolizePlayer(player)

        protocolizePlayer.registeredInventories().clear()
        val panel = Configs.getPanels()[panelName]
        if (panel != null) {
            val inventoryBuilder = InventoryBuilder(velocityGUI, player)
            inventoryBuilder.setRows(panel.rows)
            inventoryBuilder.setTitle(panel.title)
            inventoryBuilder.setEmpty(panel.empty)
            inventoryBuilder.setItems(panel.items)
            val inventory = inventoryBuilder.build()
            inventory.onClick { click: InventoryClick ->
                click.cancelled(true)
                val item = panel.items[click.slot()]
                if (item != null && item.commands != null) {
                    InventoryClickHandler(velocityGUI).execute(item.commands, click, player)
                }
            }
            protocolizePlayer.openInventory(inventory)
        }
    }
}
