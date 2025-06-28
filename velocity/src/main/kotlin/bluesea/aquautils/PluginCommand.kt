package bluesea.aquautils

import bluesea.aquautils.common.Constants.ID
import bluesea.aquautils.parser.VelocityPlayers
import bluesea.aquautils.parser.VelocityPlayersParser
import com.velocitypowered.api.proxy.Player
import kotlin.jvm.optionals.getOrNull
import net.kyori.adventure.text.Component
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.velocity.VelocityCommandManager

object PluginCommand {
    fun register(manager: VelocityCommandManager<VelocityAudience>) {
        manager.command(
            manager.commandBuilder("vkick")
                .permission("$ID.vkick")
                .required("target", VelocityPlayersParser.playersParser())
                .optional("reason", StringParser.greedyStringParser())
                .handler { ctx ->
                    val target = ctx.get<VelocityPlayers>("target")
                    val reason = ctx.optional<String>("reason")
                    target.players.forEach {
                        val player = it.source as Player
                        if (!reason.isPresent) {
                            player.disconnect(Component.translatable("multiplayer.disconnect.kicked"))
                        } else {
                            player.disconnect(Component.translatable(reason.get()))
                        }
                    }
                }
        )

        val vgetBuilder = manager.commandBuilder("vget")
            .permission("$ID.vget")
        manager.command(
            vgetBuilder
                .literal("brand")
                .required("target", VelocityPlayersParser.playersParser())
                .handler { ctx ->
                    playerInfoMod(
                        ctx,
                        { it.clientBrand },
                        "no brand"
                    )
                }
        ).command(
            vgetBuilder
                .literal("virtualhost")
                .required("target", VelocityPlayersParser.playersParser())
                .handler { ctx ->
                    playerInfoMod(
                        ctx,
                        { it.virtualHost.getOrNull() },
                        "no virtualhost",
                        { player, info ->
                            if (info.address != null) {
                                "${info.address.hostAddress}:${info.port}"
                            } else {
                                "${info.hostString}:${info.port}"
                            }
                        }
                    )
                }
        ).command(
            vgetBuilder
                .literal("address")
                .required("target", VelocityPlayersParser.playersParser())
                .handler { ctx ->
                    playerInfo(ctx) { "${it.remoteAddress}" }
                }
        ).command(
            vgetBuilder
                .literal("locale")
                .required("target", VelocityPlayersParser.playersParser())
                .handler { ctx ->
                    playerInfo(ctx) { "${it.effectiveLocale}" }
                }
        ).command(
            vgetBuilder
                .literal("version")
                .required("target", VelocityPlayersParser.playersParser())
                .handler { ctx ->
                    playerInfo(ctx) { "${it.protocolVersion}" }
                }
        ).command(
            vgetBuilder
                .literal("vvinfo")
                .required("target", VelocityPlayersParser.playersParser())
                .handler { ctx ->
                    playerInfoMod(
                        ctx,
                        { PluginVVListener.playersDetails[it] },
                        "not use vv",
                        { player, info -> PluginVVListener.playersChannel[player] + info.versionName }
                    )
                }
        )
    }

    private fun playerInfo(ctx: CommandContext<VelocityAudience>, info: (Player) -> String) {
        playerRichInfo(ctx, { true }, info, "")
    }

    private fun <T> playerInfoMod(
        ctx: CommandContext<VelocityAudience>,
        info: (Player) -> T?,
        fallbackInfo: String,
        infoMod: (Player, T) -> String = { player, info -> info as String }
    ) {
        playerRichInfo(ctx, { info.invoke(it) != null }, { infoMod.invoke(it, info.invoke(it)!!) }, fallbackInfo)
    }

    private fun playerRichInfo(
        ctx: CommandContext<VelocityAudience>,
        condition: (Player) -> Boolean,
        info: (Player) -> String,
        fallbackInfo: String
    ) {
        val target = ctx.get<VelocityPlayers>("target")
        target.players.forEach {
            val fullInfo = if (it.source is Player) {
                val player = it.source as Player
                "${player.username}: " + if (condition.invoke(player)) {
                    info.invoke(player)
                } else {
                    fallbackInfo
                }
            } else {
                fallbackInfo
            }
            ctx.sender().sendMessage(
                Component.text(fullInfo)
            )
        }
    }
}
