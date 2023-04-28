package bluesea.aquautils.common

import net.kyori.adventure.text.Component

interface CommonAudienceProvider<S> {
    fun getConsoleServerAudience(): CommonAudienceProvider<S>

    fun get(source: S): CommonAudienceProvider<S>

    fun broadcast(component: Component, permission: String)
}
