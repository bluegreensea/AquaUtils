package bluesea.aquautils.common

import bluesea.aquautils.common.parser.CommonPlayers
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.incendo.cloud.minecraft.extras.AudienceProvider

interface CommonAudienceProvider<S, C : CommonAudience<S>> : AudienceProvider<C> {
    fun parsersProvider(): CommonParsersProvider<S, C, *, *>

    fun getConsoleServerAudience(): C

    fun getAllPlayers(): CommonPlayers<C>

    fun get(source: S): C

    fun broadcast(source: S, component: Component, permission: String)

    override fun apply(source: C): Audience {
        return source.audience
    }
}
