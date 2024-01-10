package bluesea.aquautils.common

import net.kyori.adventure.text.Component

interface CommonAudienceProvider<S> {
    fun getConsoleServerAudience(): CommonAudience<S>

    fun getAllPlayersAudience(source: S): CommonAudience<S>

    fun get(source: S): CommonAudience<S>

    fun broadcast(source: S, component: Component, permission: String)
}
