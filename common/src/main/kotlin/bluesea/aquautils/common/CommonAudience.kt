package bluesea.aquautils.common

import io.netty.buffer.ByteBuf
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

abstract class CommonAudience<S> protected constructor(val source: S, val audience: Audience) {

    abstract fun hasPermission(permission: String): Boolean

    open fun sendMessage(component: Component) {
        audience.sendMessage(component)
    }

    abstract fun setServerLinks(serverLinks: List<Pair<Component, String>>)

    abstract fun sendPluginMessage(path: String, data: ByteBuf)
}
