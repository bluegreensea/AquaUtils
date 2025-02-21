package bluesea.aquautils.common

import net.kyori.adventure.text.Component
import org.incendo.cloud.minecraft.extras.AudienceProvider

interface CommonAudienceProvider<S, T : CommonAudience<S>> : AudienceProvider<S> {
    fun parsersProvider(): CommonParsersProvider<S, T, *, *>

    fun getConsoleServerAudience(): T

    fun getAllPlayersAudience(source: S): T

    fun get(source: S): T

    fun broadcast(source: S, component: Component, permission: String)
}
