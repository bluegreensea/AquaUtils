package bluesea.aquautils.common

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component

abstract class CommonAudience<C> protected constructor(audience: Audience, source: C) {
    private val audience: Audience
    val source: C

    init {
        this.audience = audience
        this.source = source
    }

    abstract fun isPlayer(): Boolean

    abstract fun hasPermission(permission: String): Boolean

    open fun sendMessage(component: Component) {
        audience.sendMessage(component)
    }
}
