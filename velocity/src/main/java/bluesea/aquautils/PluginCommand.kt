package bluesea.aquautils

import bluesea.aquautils.common.Constants.MOD_ID
import bluesea.aquautils.common.Controller
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.context.CommandContext
import cloud.commandframework.velocity.VelocityCommandManager
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component

object PluginCommand {
    fun register(manager: VelocityCommandManager<VelocityAudience>, server: ProxyServer) {
        manager.command(
            manager.commandBuilder("vkick")
                .permission("$MOD_ID.vkick")
                .argument(
                    StringArgument.builder<VelocityAudience>("target").greedy()
                        .withSuggestionsProvider { _, s ->
                            Controller.suggestionsFilteredPlayer(s, server)
                        }
                )
                .argument(
                    StringArgument.builder<VelocityAudience>("reason").greedy().asOptional()
                )
                .handler { ctx ->
                    val reason = ctx.getOptional<String>("reason")
                    val filterPlayer = getFilteredPlayer(ctx, server.allPlayers)
                    filterPlayer.forEach { player ->
                        if (reason.isPresent) {
                            player.disconnect(Component.translatable("multiplayer.disconnect.kicked"))
                        } else {
                            player.disconnect(Component.translatable(reason.get()))
                        }
                    }
                }
        )

        manager.command(
            manager.commandBuilder("vget")
                .permission("$MOD_ID.vget")
                .literal("brand")
                .argument(
                    StringArgument.builder<VelocityAudience>("target").greedy()
                        .withSuggestionsProvider { _, s ->
                            Controller.suggestionsFilteredPlayer(s, server)
                        }
                )
                .handler { ctx ->
                    val filterPlayer = getFilteredPlayer(ctx, server.allPlayers)
                    filterPlayer.forEach { player ->
                        if (player.clientBrand != null) {
                            ctx.sender.sendMessage(Component.text("${player.username}: ${player.clientBrand}"))
                        } else {
                            ctx.sender.sendMessage(Component.text("${player.username}: no brand"))
                        }
                    }
                }
        ).command(
            manager.commandBuilder("vget")
                .permission("$MOD_ID.vget")
                .literal("virtualhost")
                .argument(
                    StringArgument.builder<VelocityAudience>("target").greedy()
                        .withSuggestionsProvider { _, s ->
                            Controller.suggestionsFilteredPlayer(s, server)
                        }
                )
                .handler { ctx ->
                    val filterPlayer = getFilteredPlayer(ctx, server.allPlayers)
                    filterPlayer.forEach { player ->
                        if (player.virtualHost.isPresent) {
                            ctx.sender.sendMessage(
                                Component.text(
                                    "${player.username}: ${player.virtualHost.get().hostName}:${player.virtualHost.get().port}"
                                )
                            )
                        } else {
                            ctx.sender.sendMessage(Component.text("${player.username}: no virtualhost"))
                        }
                    }
                }
        ).command(
            manager.commandBuilder("vget")
                .permission("$MOD_ID.vget")
                .literal("address")
                .argument(
                    StringArgument.builder<VelocityAudience>("target").greedy()
                        .withSuggestionsProvider { _, s ->
                            Controller.suggestionsFilteredPlayer(s, server)
                        }
                )
                .handler { ctx ->
                    val filteredPlayer = getFilteredPlayer(ctx, server.allPlayers)
                    filteredPlayer.forEach { player ->
                        ctx.sender.sendMessage(
                            Component.text(
                                "${player.username}: ${player.remoteAddress}"
                            )
                        )
                    }
                }
        )
    }

    private fun getFilteredPlayer(ctx: CommandContext<VelocityAudience>, allPlayers: Collection<Player>): Collection<Player> {
        if (ctx.get<String>("target") == "@a") {
            return allPlayers
        } else if (ctx.get<String>("target") == "@s") {
            return listOf(ctx.sender.source as Player)
        } else {
            return allPlayers.filter { player ->
                if (player.get(Identity.NAME).isPresent) {
                    return@filter player.get(Identity.NAME).get().lowercase() == ctx.get<String>("target").lowercase()
                }
                false
            }
        }
    }
}
