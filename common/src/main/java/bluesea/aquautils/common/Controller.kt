package bluesea.aquautils.common

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import com.velocitypowered.api.command.CommandSource
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Controller {
    val LOGGER: Logger = LoggerFactory.getLogger("AquaUtils")!!
    private val lastMessage = HashMap<Component, String>()
    private val lastTimes = HashMap<Component, Int>()
    private var voteReset = false
    private val voteStrs = HashMap<Component, String?>()
    private val optionStrs = ArrayList<String>(9)
    private var kick = true
    private lateinit var voteTextComponent: Component
    fun aquautils(): LiteralCommandNode<CommandSource> {
        return LiteralArgumentBuilder.literal<CommandSource>("aquautils")
            .executes { ctx: CommandContext<CommandSource> ->
                ctx.source.sendMessage(Component.text("version: " + "\${version}"))
                Command.SINGLE_SUCCESS
            }
            .then(
                RequiredArgumentBuilder.argument<CommandSource, String>("command", StringArgumentType.string())
                    .requires { source: CommandSource -> source.hasPermission("aquautils.command") }
                    .executes { ctx: CommandContext<CommandSource> ->
                        ctx.source.sendMessage(
                            Component.text("Aqua Utils kick: $kick")
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        RequiredArgumentBuilder.argument<CommandSource, Boolean>("switch", BoolArgumentType.bool())
                            .executes { ctx: CommandContext<CommandSource> ->
                                kick = BoolArgumentType.getBool(ctx, "switch")
                                ctx.source.sendMessage(
                                    Component.text("Aqua Utils set kick: $kick")
                                )
                                LOGGER.info("set kick: $kick")
                                Command.SINGLE_SUCCESS
                            }
                    )
            ).build()
    }

    fun votereset(allPlayers: Audience): LiteralCommandNode<CommandSource> {
        return LiteralArgumentBuilder.literal<CommandSource>("votereset")
            .requires { source: CommandSource -> source.hasPermission("aquautils.votereset") }
            .executes {
                if (::voteTextComponent.isInitialized) {
                    voteStrs.clear()

                    val players = Audience.audience(allPlayers)
                    players.sendMessage(voteTextComponent)
                }

                return@executes Command.SINGLE_SUCCESS
            }
            .then(
                RequiredArgumentBuilder.argument<CommandSource, String>("options", StringArgumentType.greedyString())
                    .executes { ctx: CommandContext<CommandSource> ->
                        val options = StringArgumentType.getString(ctx, "options")
                        var textComponent = Component.empty()
                        optionStrs.clear()
                        var i = 0
                        for (option in options.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                            i++
                            textComponent = textComponent
                                .append(Component.text("+$i").color(NamedTextColor.YELLOW))
                                .append(Component.space())
                                .append(Component.text("$option "))
                            optionStrs.add(option)
                            if (i >= 9) {
                                break
                            }
                        }
                        voteStrs.clear()
                        voteReset = true
                        val players = Audience.audience(allPlayers)
                        voteTextComponent = if (ctx.source.get(Identity.DISPLAY_NAME).isPresent) {
                            Component.translatable(
                                "chat.type.text",
                                ctx.source.get(Identity.DISPLAY_NAME).get(),
                                textComponent
                            )
                        } else {
                            Component.text("[Server]").append(textComponent)
                        }
                        players.sendMessage(voteTextComponent)

                        Command.SINGLE_SUCCESS
                    }
            ).build()
    }

    fun voteget(allPlayers: Audience): LiteralCommandNode<CommandSource> {
        return LiteralArgumentBuilder.literal<CommandSource>("voteget")
            .executes { ctx: CommandContext<CommandSource> ->
                var textComponent = Component.empty()
                val allVotes = HashMap<String?, Int>()
                voteStrs.forEach { (_: Component?, v: String?) -> allVotes[v] = allVotes.getOrDefault(v, 0) + 1 }
                var firstVotes = 0
                var firstStr = StringBuilder().append("無記錄")
                for ((voteStr, votes) in allVotes) {
                    try {
                        voteStr!!.substring(1).toInt()
                    } catch (e: Exception) {
                        continue
                    }
                    if (voteStr.substring(1).toInt() > 0 &&
                        voteStr.substring(1).toInt() - 1 < optionStrs.size
                    ) {
                        textComponent = textComponent
                            .append(Component.newline())
                            .append(Component.text(voteStr).color(NamedTextColor.YELLOW).appendSpace())
                            .append(Component.text(optionStrs[voteStr.substring(1).toInt() - 1]))
                            .append(Component.text(" 人數: $votes"))
                        if (firstVotes == votes) {
                            firstStr.append(", ").append(optionStrs[voteStr.substring(1).toInt() - 1])
                        } else if (votes >= firstVotes) {
                            firstStr = StringBuilder(optionStrs[voteStr.substring(1).toInt() - 1])
                            firstVotes = votes
                        }
                    }
                }
                for ((key, voteStr) in voteStrs) {
                    val player = key as TextComponent
                    try {
                        voteStr!!.substring(1).toInt()
                        if (voteStr.substring(1).toInt() <= 0 ||
                            voteStr.substring(1).toInt() - 1 >= optionStrs.size
                        ) {
                            throw Exception()
                        }
                    } catch (ignored: Exception) {
                        textComponent = textComponent
                            .append(Component.newline())
                            .append(Component.text(player.content() + " 投了 " + voteStr))
                    }
                }

                // strings.insert(0, firstStr + "\n");
                textComponent = Component.text("投票結果: $firstStr").append(textComponent)
                ctx.source.sendMessage(
                    // Component.text("投票結果: " + (strings.toString().equals("\n") ? "無記錄" : strings.toString()))
                    textComponent
                )
                Command.SINGLE_SUCCESS
            }
            .then(
                RequiredArgumentBuilder.argument<CommandSource, String>("target", StringArgumentType.string())
                    .suggests { ctx: CommandContext<CommandSource>, builder: SuggestionsBuilder ->
                        if ("@a".contains(ctx.input.split(" ")[1].lowercase())) {
                            builder.suggest("\"@a\"")
                        }
                        allPlayers.forEachAudience { player: Audience ->
                            player.get(Identity.NAME).ifPresent { name: String ->
                                if (name.lowercase().contains(ctx.input.substring(8).lowercase())) {
                                    builder.suggest(name)
                                }
                            }
                        }
                        builder.buildFuture()
                    }
                    .executes { ctx: CommandContext<CommandSource> ->
                        val strings = StringBuilder()
                        val filteredPlayer = if (StringArgumentType.getString(ctx, "target") == "@a") {
                            allPlayers
                        } else {
                            allPlayers.filterAudience { player: Audience ->
                                if (player.get(Identity.NAME).isPresent) {
                                    return@filterAudience player.get(Identity.NAME).get() == StringArgumentType.getString(ctx, "target")
                                }
                                false
                            }
                        }
                        filteredPlayer.forEachAudience { player: Audience ->
                            if (player.get(Identity.DISPLAY_NAME).isPresent) {
                                val playerDisplayName = player.get(Identity.DISPLAY_NAME).get() as TextComponent
                                if (strings.toString() != "") strings.append("\n")
                                if (voteStrs[playerDisplayName] != null) {
                                    strings.append(playerDisplayName.content()).append(" 的投票記錄: ")
                                        .append(voteStrs[playerDisplayName])
                                } else {
                                    strings.append(playerDisplayName.content()).append(" 的投票記錄: ")
                                        .append("無記錄")
                                }
                            }
                        }
                        ctx.source.sendMessage(
                            Component.text(if (strings.toString() == "") "無任何投票記錄" else strings.toString())
                        )
                        Command.SINGLE_SUCCESS
                    }
            ).build()
    }

    fun voteset(allPlayers: Audience): LiteralCommandNode<CommandSource> {
        return LiteralArgumentBuilder.literal<CommandSource>("voteset")
            .then(
                RequiredArgumentBuilder.argument<CommandSource, String>("option", StringArgumentType.string())
                    .executes { ctx: CommandContext<CommandSource> ->
                        if (ctx.source.get(Identity.DISPLAY_NAME).isPresent) {
                            var value = StringArgumentType.getString(ctx, "option")
                            if (value[0] != '+') value = "+$value"
                            onPlayerMessage(ctx.source, value)
                        }
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        RequiredArgumentBuilder.argument<CommandSource, String>("target", StringArgumentType.string())
                            .requires { source: CommandSource -> source.hasPermission("aquautils.voteset") }
                            .suggests { ctx: CommandContext<CommandSource>, builder: SuggestionsBuilder ->
                                allPlayers.forEachAudience { player: Audience ->
                                    player.get(Identity.NAME).ifPresent { name: String ->
                                        if (name.lowercase().contains(ctx.input.split(" ")[2].lowercase())) {
                                            builder.suggest(name)
                                        }
                                    }
                                }
                                builder.buildFuture()
                            }
                            .executes { ctx: CommandContext<CommandSource> ->
                                val filteredPlayer = allPlayers.filterAudience { player: Audience ->
                                    if (player.get(Identity.NAME).isPresent) {
                                        return@filterAudience player.get(Identity.NAME).get() == StringArgumentType.getString(ctx, "target")
                                    }
                                    false
                                }
                                filteredPlayer.forEachAudience { player: Audience ->
                                    if (player.get(Identity.DISPLAY_NAME).isPresent) {
                                        val index = player.get(Identity.DISPLAY_NAME).get()
                                        var value = StringArgumentType.getString(ctx, "option")
                                        if (value[0] != '+') value = "+$value"
                                        voteStrs[index as TextComponent] = value
                                        ctx.source.sendMessage(
                                            Component.text("已將 ").append(index)
                                                .append(Component.text(" 的投票設定為 $value"))
                                        )
                                    }
                                }
                                Command.SINGLE_SUCCESS
                            }
                    )
            ).build()
    }

    fun onPlayerMessage(player: Audience, message: String): Boolean {
        if (player.get(Identity.NAME).isPresent) {
            val index: Component = Component.text(player.get(Identity.NAME).get())
            if (message.contains("+")) {
                if (message.substring(message.indexOf("+")).length >= 2 && voteReset) {
                    var value = message.substring(message.indexOf("+"), message.indexOf("+") + 2)
                    if (value.toCharArray()[1] in '\ud800'..'\ue000') {
                        value = message.substring(message.indexOf("+"), message.indexOf("+") + 3)
                    }
                    if (voteStrs[index] == null || voteStrs[index] != value) {
                        voteStrs[index] = value
                        lastMessage.remove(index)
                        player.sendMessage(
                            Component.text("已將自己的投票改變為 $value")
                        )
                    }
                }
            }

            if (message != lastMessage.getOrDefault(index, "")) {
                lastMessage[index] = message
                lastTimes[index] = 0
            } else {
                if (lastTimes[index]!! < 2) {
                    lastTimes[index] = lastTimes[index]!! + 1
                } else {
                    if (kick) {
                        lastTimes[index] = 0
                        return true
                    }
                }
            }
        }
        return false
    }
}
