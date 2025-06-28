package bluesea.aquautils.common.parser

import bluesea.aquautils.common.CommonAudience
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

open class CommonPlayers<C : CommonAudience<*>>(val players: List<C>) {
    fun forEachAudience(function: (Audience) -> Unit) {
        players.forEach {
            function.invoke(it.audience)
        }
    }

    fun sendMessage(component: Component) {
        players.forEach { it.sendMessage(component) }
    }

    fun setServerLinks(serverLinks: List<Pair<Component, String>>) {
        players.forEach { it.setServerLinks(serverLinks) }
    }
}
