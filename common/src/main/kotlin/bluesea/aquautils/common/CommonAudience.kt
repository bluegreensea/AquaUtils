package bluesea.aquautils.common

import io.netty.buffer.ByteBuf
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

abstract class CommonAudience<C> protected constructor(val audience: Audience, val source: C) {
    abstract val isPlayer: Boolean

    abstract fun hasPermission(permission: String): Boolean

    open fun sendMessage(component: Component) {
        audience.sendMessage(component)
    }

    abstract fun setServerLinks(serverLinks: List<Pair<Component, String>>)

    abstract fun sendPluginMessage(path: String, data: ByteBuf)
}
