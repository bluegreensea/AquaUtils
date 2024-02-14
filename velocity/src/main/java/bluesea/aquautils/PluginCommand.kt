package bluesea.aquautils

import bluesea.aquautils.common.Constants.MOD_ID
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import org.incendo.cloud.velocity.VelocityCommandManager

object PluginCommand {
    fun register(manager: VelocityCommandManager<VelocityAudience>, server: ProxyServer) {
        manager.command(
            manager.commandBuilder("vkick")
                .permission("$MOD_ID.vkick")
                .required("target", StringParser.quotedStringParser()) { c, s ->
                    SuggestionProvider
                        .suggesting<VelocityAudience>(suggestionsFilteredPlayer(s.peekString(), server))
                        .suggestionsFuture(c, s)
                }
                .optional("reason", StringParser.greedyStringParser())
                .handler { ctx ->
                    val filterPlayer = getFilteredPlayer(ctx, server.allPlayers)
                    val reason = ctx.optional<String>("reason")
                    filterPlayer.forEach { player ->
                        if (!reason.isPresent) {
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
                .required("target", StringParser.quotedStringParser()) { c, s ->
                    SuggestionProvider
                        .suggesting<VelocityAudience>(suggestionsFilteredPlayer(s.peekString(), server))
                        .suggestionsFuture(c, s)
                }
                .handler { ctx ->
                    val filterPlayer = getFilteredPlayer(ctx, server.allPlayers)
                    filterPlayer.forEach { player ->
                        if (player.clientBrand != null) {
                            ctx.sender().sendMessage(Component.text("${player.username}: ${player.clientBrand}"))
                        } else {
                            ctx.sender().sendMessage(Component.text("${player.username}: no brand"))
                        }
                    }
                }
        ).command(
            manager.commandBuilder("vget")
                .permission("$MOD_ID.vget")
                .literal("virtualhost")
                .required("target", StringParser.quotedStringParser()) { c, s ->
                    SuggestionProvider
                        .suggesting<VelocityAudience>(suggestionsFilteredPlayer(s.peekString(), server))
                        .suggestionsFuture(c, s)
                }
                .handler { ctx ->
                    val filterPlayer = getFilteredPlayer(ctx, server.allPlayers)
                    filterPlayer.forEach { player ->
                        if (player.virtualHost.isPresent) {
                            ctx.sender().sendMessage(
                                Component.text(
                                    "${player.username}: ${player.virtualHost.get().hostName}:${player.virtualHost.get().port}"
                                )
                            )
                        } else {
                            ctx.sender().sendMessage(Component.text("${player.username}: no virtualhost"))
                        }
                    }
                }
        ).command(
            manager.commandBuilder("vget")
                .permission("$MOD_ID.vget")
                .literal("address")
                .required("target", StringParser.quotedStringParser()) { c, s ->
                    SuggestionProvider
                        .suggesting<VelocityAudience>(suggestionsFilteredPlayer(s.peekString(), server))
                        .suggestionsFuture(c, s)
                }
                .handler { ctx ->
                    val filteredPlayer = getFilteredPlayer(ctx, server.allPlayers)
                    filteredPlayer.forEach { player ->
                        ctx.sender().sendMessage(
                            Component.text(
                                "${player.username}: ${player.remoteAddress}"
                            )
                        )
                    }
                }
        )
    }

    private fun suggestionsFilteredPlayer(input: String, allPlayers: Audience): ArrayList<Suggestion> {
        val completions = arrayListOf<Suggestion>()
        if ("\"@a\"".contains(input.lowercase())) {
            completions.add(Suggestion.simple("\"@a\""))
        }
        if ("\"@s\"".contains(input.lowercase())) {
            completions.add(Suggestion.simple("\"@s\""))
        }
        allPlayers.forEachAudience { player: Audience ->
            player.get(Identity.NAME).ifPresent { name: String ->
                if (name.lowercase().contains(input.lowercase())) {
                    completions.add(Suggestion.simple(name))
                }
            }
        }
        return completions
    }

    private fun getFilteredPlayer(ctx: CommandContext<VelocityAudience>, allPlayers: Collection<Player>): Collection<Player> {
        val target = ctx.get<String>("target").replace("\"", "")
        when (target) {
            "@a" -> return allPlayers
            "@s" -> return listOf(ctx.sender().source as Player)
            else -> {
                return allPlayers.filter { player ->
                    if (player.get(Identity.NAME).isPresent) {
                        return@filter player.get(Identity.NAME).get().lowercase() == target.lowercase()
                    }
                    false
                }
            }
        }
    }
}
