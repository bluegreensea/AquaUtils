package bluesea.aquautils.common

import bluesea.aquautils.common.Constants.MOD_ID
import bluesea.aquautils.fetcher.YoutubeFetcher
import java.util.UUID
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.BooleanParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Controller {
    val LOGGER: Logger = LoggerFactory.getLogger(Constants.MOD_NAME)
    private val lastMessage = HashMap<UUID, String>()
    private val lastTimes = HashMap<UUID, Int>()
    private var voteReset = false
    private val voteData = VoteData(
        voteStrings = HashMap(),
        optionStrings = ArrayList(9),
        winnerOption = Component.text().content("無紀錄"),
        result = Component.text(),
        resultForOther = Component.text()
    )
    private const val SILENT_OPTION_STRING = "--silent"
    private val voteStringColor = NamedTextColor.YELLOW
    private val voteOptionColor = NamedTextColor.GREEN
    private var voteTextComponent = Component.text()
    var kick = true
    private lateinit var videoId: String
    var ytChatLooper: Thread? = null

    fun <M : CommandManager<C>, C : CommonAudience<S>, S> register(manager: M, provider: CommonAudienceProvider<S>) {
        manager.command(
            manager.commandBuilder(MOD_ID)
                .handler {
                    aquautils(it)
                }
        ).command(
            manager.commandBuilder(MOD_ID)
                .permission("$MOD_ID.command")
                .required("command", StringParser.stringParser())
                .optional("switch", BooleanParser.booleanParser())
                .handler {
                    aquautils(it)
                }
        )

        manager.command(
            manager.commandBuilder("votereset")
                .permission("$MOD_ID.votereset")
                .optional("options", StringParser.greedyStringParser()) { c, s ->
                    val suggestions = arrayListOf<Suggestion>()
                    if (!s.peekString().contains(SILENT_OPTION_STRING)) {
                        suggestions.add(Suggestion.simple(SILENT_OPTION_STRING))
                    }
                    SuggestionProvider
                        .suggesting<C>(suggestions)
                        .suggestionsFuture(c, s)
                }
                .handler {
                    votereset(it, provider.getConsoleServerAudience(), provider.getAllPlayersAudience(it.sender().source))
                }
        )

        manager.command(
            manager.commandBuilder("voteget")
                .optional("target", StringParser.greedyStringParser()) { c, s ->
                    // TODO: suggestion available vote options
                    SuggestionProvider
                        .suggesting<C>(
                            suggestionsFilteredPlayer(s.peekString(), provider.getAllPlayersAudience(c.sender().source))
                        )
                        .suggestionsFuture(c, s)
                }
                .handler {
                    voteget(it, provider.getAllPlayersAudience(it.sender().source))
                }
        )

        manager.command(
            manager.commandBuilder("voteset")
                .permission("$MOD_ID.voteset")
                .required("value", StringParser.stringParser())
                .optional("target", StringParser.greedyStringParser()) { c, s ->
                    SuggestionProvider
                        .suggesting<C>(
                            suggestionsFilteredPlayer(s.peekString(), provider.getAllPlayersAudience(c.sender().source))
                        )
                        .suggestionsFuture(c, s)
                }
                .handler {
                    voteset(it, provider.getAllPlayersAudience(it.sender().source))
                }
        )

        manager.command(
            manager.commandBuilder("vytchat")
                .permission("$MOD_ID.vytchat")
                .literal("start")
                .required("videoid", StringParser.greedyStringParser())
                .handler {
                    vytchat(it, provider.getAllPlayersAudience(it.sender().source))
                }
        ).command(
            manager.commandBuilder("vytchat")
                .permission("$MOD_ID.vytchat")
                .literal("stop")
                .handler {
                    vytchat(it, provider.getAllPlayersAudience(it.sender().source))
                }
        ).command(
            manager.commandBuilder("vytchat")
                .literal("url")
                .handler {
                    vytchat(it, provider.getAllPlayersAudience(it.sender().source))
                }
        ).command(
            manager.commandBuilder("vytchat")
                .permission("$MOD_ID.vytchat")
                .literal("urlall")
                .handler {
                    vytchat(it, provider.getAllPlayersAudience(it.sender().source))
                }
        )
    }

    private fun <C : CommonAudience<S>, S> aquautils(ctx: CommandContext<C>) {
        val command = ctx.optional<String>("command")
        val switch = ctx.optional<Boolean>("switch")

        if (command.isEmpty) {
            ctx.sender().sendMessage(Component.text("version: ${Constants.MOD_VERSION}"))
        } else if (ctx.hasPermission("$MOD_ID.command")) {
            if (switch.isEmpty) {
                ctx.sender().sendMessage(
                    Component.text("Aqua Utils kick: $kick")
                )
            } else {
                kick = switch.get()
                ctx.sender().sendMessage(
                    Component.text("Aqua Utils set kick: $kick")
                )
                LOGGER.info("set kick: $kick")
            }
        }
    }

    private fun <C : CommonAudience<*>> votereset(ctx: CommandContext<C>, console: CommonAudience<*>, allPlayers: CommonAudience<*>) {
        val options = ctx.optional<String>("options")

        val players = allPlayers.audience
        if (options.isEmpty || options.get() == SILENT_OPTION_STRING) {
            if (voteTextComponent.build().children().isNotEmpty()) {
                voteData.voteStrings.clear()
                voteCounting(allPlayers)

                if (options.isEmpty) {
                    players.sendMessage(voteTextComponent.build())
                }
            } else {
                ctx.sender().sendMessage(
                    Component.text("需要至少重置一次")
                )
            }
        } else {
            voteData.optionStrings.clear()
            var silent = false
            val textComponent = Component.text()
            var i = 0
            for (option in options.get().split(" ")) {
                if (option == SILENT_OPTION_STRING) {
                    silent = true
                    continue
                }
                i++
                voteData.optionStrings.add(option)
                textComponent
                    .append(Component.text("+$i").color(voteStringColor))
                    .append(Component.space())
                    .append(Component.text("$option ").color(voteOptionColor))
                if (i >= 9) {
                    break
                }
            }
            voteData.voteStrings.clear()
            voteCounting(allPlayers)
            voteReset = true
            voteTextComponent = Component.text()
            if (ctx.sender().audience.get(Identity.DISPLAY_NAME).isPresent) {
                voteTextComponent.append(
                    Component.translatable(
                        "chat.type.text",
                        ctx.sender().audience.get(Identity.DISPLAY_NAME).get(),
                        textComponent.build()
                    )
                )
            } else {
                voteTextComponent.append(Component.text("[Server] ")).append(textComponent.build())
            }
            if (!silent) {
                players.sendMessage(voteTextComponent.build())
                console.sendMessage(
                    ctx.sender().audience.get(Identity.DISPLAY_NAME).get()
                        .append(Component.text(": "))
                        .append(textComponent.build())
                )
            }
        }
    }

    // TODO: voteData network
    private fun voteCounting(allPlayers: CommonAudience<*>, index: UUID? = null, value: String? = null) {
        if (index != null && value != null) {
        }
        voteData.result = Component.text()
        voteData.resultForOther = Component.text()
        val votesByName = HashMap<String, Int>()
        voteData.voteStrings.forEach { (uuid, optionStr) ->
            try {
                if (optionStr.substring(1).toInt() !in 1..voteData.optionStrings.size) {
                    throw Exception()
                }
            } catch (_: Exception) {
                allPlayers.audience.forEachAudience { player ->
                    if (player.get(Identity.UUID).isPresent && player.get(Identity.UUID).get() == uuid) {
                        voteData.resultForOther.append(Component.newline())
                            .append(player.get(Identity.DISPLAY_NAME).get())
                            .append(Component.text(" 投了 "))
                            .append(Component.text(optionStr).color(voteStringColor))
                    }
                }
                return@forEach
            }
            votesByName[optionStr] = votesByName.getOrDefault(optionStr, 0) + 1
        }
        var winnerOptionVotes = 0
        voteData.winnerOption = Component.text().content("無紀錄")
        for ((voteString, votes) in votesByName) {
            if (voteString.substring(1).toInt() in 1..voteData.optionStrings.size) {
                val optionString = voteData.optionStrings[voteString.substring(1).toInt() - 1]
                voteData.result.append(Component.newline())
                    .append(Component.text(voteString).color(voteStringColor).appendSpace())
                    .append(Component.text(optionString).color(voteOptionColor))
                    .append(Component.text(" 票數: $votes"))
                if (winnerOptionVotes == votes) {
                    voteData.winnerOption.append(Component.text(", "))
                        .append(Component.text(optionString))
                } else if (votes >= winnerOptionVotes) {
                    voteData.winnerOption = Component.text()
                        .content(optionString).color(voteOptionColor)
                    winnerOptionVotes = votes
                }
            }
        }

        // val buf = Unpooled.copiedBuffer("Test", CharsetUtil.UTF_8)
        // allPlayers.sendPluginMessage("test", buf)
    }

    private fun <C : CommonAudience<S>, S> voteget(ctx: CommandContext<C>, allPlayers: CommonAudience<S>) {
        val target = ctx.optional<String>("target")

        if (target.isEmpty) {
            ctx.sender().sendMessage(
                Component.text("投票結果: ")
                    .append(voteData.winnerOption.build())
                    .append(voteData.result.build())
                    .append(voteData.resultForOther.build())
            )
        } else {
            val result = Component.text()
            if (target.get()[0] == '+') {
                allPlayers.audience.forEachAudience { player ->
                    if (player.get(Identity.DISPLAY_NAME).isPresent) {
                        val playerUuid = player.get(Identity.UUID).get()
                        val playerDisplayName = player.get(Identity.DISPLAY_NAME).get() as TextComponent
                        if (voteData.voteStrings[playerUuid] != null && voteData.voteStrings[playerUuid] == target.get()) {
                            if (result.content() == "") {
                                result.content("投 ")
                                    .append(Component.text(target.get()).color(voteStringColor))
                                    .append(Component.text(" 的玩家: "))
                                    .append(Component.newline())
                            } else {
                                result.append(Component.text(", "))
                            }
                            result.append(playerDisplayName)
                        }
                    }
                }
            } else {
                val filteredPlayer = getFilteredPlayer(ctx, allPlayers)
                filteredPlayer.forEachAudience { player ->
                    if (player.get(Identity.DISPLAY_NAME).isPresent) {
                        val playerUuid = player.get(Identity.UUID).get()
                        val playerDisplayName = player.get(Identity.DISPLAY_NAME).get() as TextComponent
                        if (voteData.voteStrings[playerUuid] != null) {
                            if (result.children().isNotEmpty()) result.append(Component.newline())
                            result.append(playerDisplayName)
                                .append(Component.text(" 的投票紀錄: "))
                                .append(Component.text(voteData.voteStrings[playerUuid]!!).color(voteStringColor))
                        } else if (target.get() != "@a") {
                            result.append(playerDisplayName)
                                .append(Component.text(" 的投票紀錄: "))
                                .append(Component.text("無紀錄"))
                        }
                    }
                }
            }
            ctx.sender().sendMessage(
                if (result.build().children().isNotEmpty()) result.build() else Component.text("無任何投票紀錄")
            )
        }
    }

    private fun <C : CommonAudience<S>, S> voteset(ctx: CommandContext<C>, allPlayers: CommonAudience<S>) {
        val target = ctx.optional<String>("target")
        var value = ctx.get<String>("value")
        if (value[0] != '+') value = "+$value"

        if (target.isEmpty) {
            onPlayerVote(ctx.sender().audience, ctx.sender().audience, value, allPlayers)
        } else {
            val filteredPlayer = getFilteredPlayer(ctx, allPlayers)
            filteredPlayer.forEachAudience { player: Audience ->
                onPlayerVote(ctx.sender().audience, player, value, allPlayers)
            }
        }
    }

    private fun <C : CommonAudience<S>, S> vytchat(ctx: CommandContext<C>, allPlayers: CommonAudience<S>) {
        // val command = ctx.rawInput[1]
        val command = ctx.rawInput().input().split(" ")[1]
        val videoid = ctx.optional<String>("videoid")

        val ytChatPrefix = Component.text("[YTChat] ").color(NamedTextColor.RED)
        when (command) {
            "start" -> {
                videoId = videoid.get()
                videoId = videoId.replace("https://www.youtube.com/watch?v=", "")
                if (ytChatLooper == null || !ytChatLooper!!.isAlive) {
                    ytChatLooper = Thread {
                        YoutubeFetcher(
                            allPlayers.audience,
                            videoId
                        ).fetch()
                    }
                    ytChatLooper!!.start()
                    allPlayers.audience.sendMessage(
                        ytChatPrefix.append(Component.text("啟動成功!"))
                    )
                    LOGGER.info("YTChat 啟動成功!")
                } else {
                    ctx.sender().sendMessage(
                        ytChatPrefix.append(Component.text("啟動失敗－已經啟動!"))
                    )
                }
            }
            "stop" -> {
                if (ytChatLooper != null && ytChatLooper!!.isAlive) {
                    ytChatLooper!!.interrupt()
                    LOGGER.info("YTChat 已關閉!")
                } else {
                    ctx.sender().sendMessage(
                        ytChatPrefix.append(Component.text("尚未啟動!"))
                    )
                }
            }
            "url", "urlall" -> {
                if (ytChatLooper != null && ytChatLooper!!.isAlive && Controller::videoId.isInitialized) {
                    val url = "https://www.youtube.com/watch?v=$videoId"
                    val urlMessage = ytChatPrefix
                        .append(Component.text("直播網址: "))
                        .append(Component.text(url).clickEvent(ClickEvent.openUrl(url)))
                    if (!command.contains("all")) {
                        ctx.sender().sendMessage(urlMessage)
                    } else {
                        allPlayers.audience.sendMessage(urlMessage)
                    }
                } else {
                    ctx.sender().sendMessage(
                        ytChatPrefix.append(Component.text("尚未啟動!"))
                    )
                }
            }
        }
    }

    fun suggestionsFilteredPlayer(input: String, allPlayers: Audience): ArrayList<Suggestion> {
        val completions = arrayListOf<Suggestion>()
        if ("@a".contains(input.lowercase())) {
            completions.add(Suggestion.simple("@a"))
        }
        if ("@s".contains(input.lowercase())) {
            completions.add(Suggestion.simple("@s"))
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

    private fun suggestionsFilteredPlayer(input: String, allPlayers: CommonAudience<*>): ArrayList<Suggestion> {
        return suggestionsFilteredPlayer(input, allPlayers.audience)
    }

    private fun <C : CommonAudience<S>, S> getFilteredPlayer(ctx: CommandContext<C>, allPlayers: CommonAudience<S>): Audience {
        when (val target = ctx.get<String>("target")) {
            "@a" -> return allPlayers.audience
            "@s" -> return ctx.sender().audience
            else -> {
                return allPlayers.audience.filterAudience { player ->
                    if (player.get(Identity.NAME).isPresent) {
                        return@filterAudience player.get(Identity.NAME).get().lowercase() == target.lowercase()
                    }
                    false
                }
            }
        }
    }

    private fun <S> onPlayerVote(sender: Audience, player: Audience, message: String, allPlayers: CommonAudience<S>) {
        if (player.get(Identity.UUID).isPresent) {
            val index = player.get(Identity.UUID).get()

            var value = message.substring(message.indexOf("+"), message.indexOf("+") + 2)
            if (value.toCharArray()[1] in '\ud800'..'\udfff') {
                value = message.substring(message.indexOf("+"), message.indexOf("+") + 3)
            }

            if (voteData.voteStrings[index] == null || voteData.voteStrings[index] != value) {
                voteData.voteStrings[index] = value

                val voteNumber = Component.text(value).color(voteStringColor)
                if (sender == player) {
                    sender.sendMessage(Component.text("已將自己的投票改變為 ").append(voteNumber))
                } else {
                    if (player.get(Identity.DISPLAY_NAME).isPresent) {
                        sender.sendMessage(
                            Component.text("已將 ")
                                .append(player.get(Identity.DISPLAY_NAME).get())
                                .append(Component.text(" 的投票設定為 "))
                                .append(voteNumber)
                        )
                    }
                }

                voteCounting(allPlayers, index, value)
            }
        }
    }

    fun <S> onPlayerMessage(player: Audience, message: String, allPlayers: CommonAudience<S>): Boolean {
        if (player.get(Identity.UUID).isPresent) {
            val index = player.get(Identity.UUID).get()
            if (message.contains("+")) {
                if (message.substring(message.indexOf("+")).length >= 2 && voteReset) {
                    onPlayerVote(player, player, message, allPlayers)
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
