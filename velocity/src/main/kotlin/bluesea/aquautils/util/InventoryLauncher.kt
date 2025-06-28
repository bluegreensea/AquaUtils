package bluesea.aquautils.util

import com.james090500.VelocityGUI.VelocityGUI
import com.james090500.VelocityGUI.config.Configs
import com.james090500.VelocityGUI.helpers.InventoryBuilder
import com.james090500.VelocityGUI.helpers.InventoryClickHandler
import com.velocitypowered.api.proxy.Player
import dev.simplix.protocolize.api.Protocolize

class InventoryLauncher(private val velocityGUI: VelocityGUI) {
    companion object {
        val NO_CUSTOM_ITEM = { _: Configs.Panel, _: InventoryBuilder -> Pair(-1) {} }
    }

    fun execute(panelName: String, player: Player, setCustomItem: (Configs.Panel, InventoryBuilder) -> Pair<Int, () -> Unit> = NO_CUSTOM_ITEM) {
        val protocolizePlayer = Protocolize.playerProvider().player(player.uniqueId)

        protocolizePlayer.registeredInventories().clear()
        val panel = Configs.getPanels()[panelName] ?: return
        val inventoryBuilder = InventoryBuilder(velocityGUI, player)
        inventoryBuilder.setRows(panel.rows)
        inventoryBuilder.setTitle(panel.title)
        inventoryBuilder.setEmpty(panel.empty)
        val customAction = setCustomItem.invoke(panel, inventoryBuilder)
        inventoryBuilder.setItems(panel.items)
        val inventory = inventoryBuilder.build()
        inventory.onClick { click ->
            click.cancelled(true)
            val item = panel.items[click.slot()]
            if (item != null && item.commands != null) {
                InventoryClickHandler(velocityGUI).execute(item.commands, click)
            } else if (click.slot() == customAction.first) {
                customAction.second.invoke()
            }
        }
        protocolizePlayer.openInventory(inventory)
    }
}
