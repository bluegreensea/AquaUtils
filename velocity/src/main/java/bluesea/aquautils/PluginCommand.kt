package bluesea.aquautils

import bluesea.aquautils.common.LegacyController
import bluesea.aquautils.fetcher.YoutubeFetcher
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object PluginCommand {
    fun vkickCreateBrigadierCommand(server: ProxyServer): BrigadierCommand {
        val commandNode = LiteralArgumentBuilder
            .literal<CommandSource>("vkick")
            .requires { source: CommandSource -> source.hasPermission("aquautils.vkick") }
            .then(
                RequiredArgumentBuilder.argument<CommandSource, String>("target", StringArgumentType.string())
                    .suggests { ctx: CommandContext<CommandSource>, builder: SuggestionsBuilder ->
                        if ("@a".contains(ctx.input.substring(6).lowercase())) {
                            builder.suggest("\"@a\"")
                        }
                        server.allPlayers.forEach {
                            if (it.username.lowercase().contains(ctx.input.substring(6).lowercase())) {
                                builder.suggest(it.username)
                            }
                        }
                        builder.buildFuture()
                    }
                    .executes { ctx: CommandContext<CommandSource> ->
                        if (StringArgumentType.getString(ctx, "target") == "@a") {
                            server.allPlayers.forEach {
                                it.disconnect(Component.translatable("multiplayer.disconnect.kicked"))
                            }
                        } else {
                            server.getPlayer(StringArgumentType.getString(ctx, "target")).ifPresent {
                                it.disconnect(Component.translatable("multiplayer.disconnect.kicked"))
                            }
                        }
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        RequiredArgumentBuilder.argument<CommandSource, String>("reason", StringArgumentType.greedyString())
                            .executes { ctx: CommandContext<CommandSource> ->
                                if (StringArgumentType.getString(ctx, "target") == "@a") {
                                    server.allPlayers.forEach {
                                        it.disconnect(
                                            Component.translatable(
                                                StringArgumentType.getString(ctx, "reason")
                                            )
                                        )
                                    }
                                } else {
                                    server.getPlayer(StringArgumentType.getString(ctx, "target")).ifPresent {
                                        it.disconnect(
                                            Component.translatable(
                                                StringArgumentType.getString(ctx, "reason")
                                            )
                                        )
                                    }
                                }
                                Command.SINGLE_SUCCESS
                            }
                    )
            ).build()
        return BrigadierCommand(commandNode)
    }

    fun vgetbrandCreateBrigadierCommand(server: ProxyServer): BrigadierCommand {
        val commandNode = LiteralArgumentBuilder
            .literal<CommandSource>("vgetbrand")
            .requires { source: CommandSource -> source.hasPermission("aquautils.vgetbrand") }
            .then(
                RequiredArgumentBuilder.argument<CommandSource, String>("targets", StringArgumentType.word())
                    .suggests { ctx: CommandContext<CommandSource>, builder: SuggestionsBuilder ->
                        server.allPlayers.forEach {
                            if (it.username.lowercase().contains(ctx.input.substring(10).lowercase())) {
                                builder.suggest(it.username)
                            }
                        }
                        builder.buildFuture()
                    }
                    .executes { ctx: CommandContext<CommandSource> ->
                        server.getPlayer(StringArgumentType.getString(ctx, "targets")).ifPresent { player: Player ->
                            if (player.clientBrand != null) {
                                ctx.source.sendMessage(Component.text(player.clientBrand!!))
                            } else {
                                ctx.source.sendMessage(Component.text("no brand"))
                            }
                        }
                        Command.SINGLE_SUCCESS
                    }
            ).build()
        return BrigadierCommand(commandNode)
    }

    fun vytchat(server: ProxyServer): BrigadierCommand {
        val commandNode = LiteralArgumentBuilder
            .literal<CommandSource>("vytchat")
            .requires { source: CommandSource -> source.hasPermission("aquautils.vytchat") }
            .then(
                RequiredArgumentBuilder.argument<CommandSource, String>("command", StringArgumentType.word())
                    .suggests { _, builder ->
                        builder.suggest("start")
                        builder.suggest("stop")

                        builder.buildFuture()
                    }
                    .executes { ctx ->
                        if (LegacyController.ytChatLooper != null && LegacyController.ytChatLooper!!.isAlive) {
                            LegacyController.ytChatLooper!!.interrupt()
                            LegacyController.LOGGER.info("YTChat 已關閉!")
                        } else {
                            ctx.source.sendMessage(
                                Component.empty()
                                    .append(Component.text("[YTChat] ").color(NamedTextColor.RED))
                                    .append(Component.text("尚未啟動!"))
                            )
                        }

                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        RequiredArgumentBuilder.argument<CommandSource, String>("videoid", StringArgumentType.word())
                            .executes { ctx: CommandContext<CommandSource> ->
                                if (LegacyController.ytChatLooper == null || !LegacyController.ytChatLooper!!.isAlive) {
                                    LegacyController.ytChatLooper = Thread {
                                        YoutubeFetcher(
                                            server,
                                            StringArgumentType.getString(ctx, "videoid")
                                        ).fetch()
                                    }
                                    LegacyController.ytChatLooper!!.start()
                                    server.sendMessage(
                                        Component.empty()
                                            .append(Component.text("[YTChat] ").color(NamedTextColor.RED))
                                            .append(Component.text("啟動成功!"))
                                    )
                                    LegacyController.LOGGER.info("YTChat 啟動成功!")
                                } else {
                                    ctx.source.sendMessage(
                                        Component.empty()
                                            .append(Component.text("[YTChat] ").color(NamedTextColor.RED))
                                            .append(Component.text("啟動失敗－已經啟動!"))
                                    )
                                }

                                Command.SINGLE_SUCCESS
                            }
                    )
            ).build()
        return BrigadierCommand(commandNode)
    }
}
