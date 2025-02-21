package bluesea.aquautils.common

import bluesea.aquautils.common.Constants.ID
import bluesea.aquautils.common.parser.CommonPlayers
import bluesea.aquautils.common.parser.CommonVoteOption
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
    val LOGGER: Logger = LoggerFactory.getLogger(Constants.NAME)
    private val lastMessage = HashMap<UUID, String>()
    private val lastTimes = HashMap<UUID, Int>()
    private var voteReset = false
    val voteData = VoteData(
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
    private lateinit var ytFetcher: YoutubeFetcher<*>
    var ytChatLooper: Thread? = null
    val serverLinks = mutableMapOf<String, Pair<Component, String>>()

    init {
        serverLinks["dc"] = Pair(Component.text("DC 群"), "https://discord.gg/xxxxxxx")
    }

    fun getServerLinks(): List<Pair<Component, String>> {
        return serverLinks.map { (_, v) -> v }
    }

    fun <M : CommandManager<T>, T : CommonAudience<S>, S> register(manager: M, provider: CommonAudienceProvider<S, T>) {
        manager.command(
            manager.commandBuilder(ID)
                .handler {
                    aquautils(it)
                }
        ).command(
            manager.commandBuilder(ID)
                .permission("$ID.command")
                .required("command", StringParser.stringParser())
                .optional("switch", BooleanParser.booleanParser())
                .handler {
                    aquautils(it)
                }
        )

        manager.command(
            manager.commandBuilder("votereset")
                .permission("$ID.votereset")
                .optional("options", StringParser.greedyStringParser()) { c, s ->
                    // TODO: options history
                    val suggestions = arrayListOf<Suggestion>()
                    if (!s.peekString().contains(SILENT_OPTION_STRING)) {
                        suggestions.add(Suggestion.suggestion(SILENT_OPTION_STRING))
                    }
                    SuggestionProvider
                        .suggesting<T>(suggestions)
                        .suggestionsFuture(c, s)
                }
                .handler {
                    votereset(it, provider.getConsoleServerAudience(), provider.getAllPlayersAudience(it.sender().source))
                }
        )

        manager.command(
            manager.commandBuilder("voteget")
                .optional("option", provider.parsersProvider().voteOptionParser())
                .handler {
                    voteget(it, provider.getAllPlayersAudience(it.sender().source))
                }
        )

        manager.command(
            manager.commandBuilder("voteset")
                .permission("$ID.voteset")
                .required("value", StringParser.stringParser())
                .optional("target", provider.parsersProvider().playersParser())
                .handler {
                    voteset(it, provider.getAllPlayersAudience(it.sender().source))
                }
        )

        val vytchatBuilder = manager.commandBuilder("vytchat")
            .handler {
                vytchat(it, provider.getAllPlayersAudience(it.sender().source))
            }
        manager.command(
            vytchatBuilder
                .permission("$ID.vytchat")
                .literal("start")
                .required("videoid", StringParser.greedyStringParser())
        ).command(
            vytchatBuilder
                .permission("$ID.vytchat")
                .literal("stop")
        ).command(
            vytchatBuilder
                .permission("$ID.vytchat")
                .literal("urlbroadcast")
        ).command(
            vytchatBuilder
                .literal("url")
        )
    }

    @Suppress("SpellCheckingInspection")
    private fun <C : CommonAudience<S>, S> aquautils(ctx: CommandContext<C>) {
        val command = ctx.optional<String>("command")
        val switch = ctx.optional<Boolean>("switch")

        if (command.isEmpty) {
            ctx.sender().sendMessage(Component.text("version: ${Constants.VERSION}"))
        } else if (ctx.hasPermission("$ID.command")) {
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

    @Suppress("SpellCheckingInspection")
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
        voteData.voteStrings.forEach { (uuid, votedString) ->
            try {
                val voteValue = votedString.substring(1).toInt()
                if (voteValue !in 1..voteData.optionStrings.size) {
                    throw NumberFormatException()
                }
            } catch (_: NumberFormatException) {
                allPlayers.audience.forEachAudience { player ->
                    if (player.get(Identity.UUID).isPresent && player.get(Identity.UUID).get() == uuid) {
                        voteData.resultForOther.append(Component.newline())
                            .append(player.get(Identity.DISPLAY_NAME).get())
                            .append(Component.text(" 投了 "))
                            .append(Component.text(votedString).color(voteStringColor))
                    }
                }
                return@forEach
            }
            votesByName[votedString] = votesByName.getOrDefault(votedString, 0) + 1
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

    @Suppress("SpellCheckingInspection")
    private fun <C : CommonAudience<S>, S> voteget(ctx: CommandContext<C>, allPlayers: CommonAudience<S>) {
        val voteOption = ctx.optional<CommonVoteOption<C>>("option")

        if (voteOption.isEmpty) {
            ctx.sender().sendMessage(
                Component.text("投票結果: ")
                    .append(voteData.winnerOption.build())
                    .append(voteData.result.build())
                    .append(voteData.resultForOther.build())
            )
        } else {
            val result = Component.text()
            val option = voteOption.get().option
            val players = voteOption.get().players
            if (players == null) {
                allPlayers.audience.forEachAudience { player ->
                    if (player.get(Identity.DISPLAY_NAME).isPresent) {
                        val playerUuid = player.get(Identity.UUID).get()
                        val playerDisplayName = player.get(Identity.DISPLAY_NAME).get() as TextComponent
                        if (voteData.voteStrings[playerUuid] != null && voteData.voteStrings[playerUuid] == option) {
                            if (result.content() == "") {
                                result.content("投 ")
                                    .append(Component.text(option).color(voteStringColor))
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
                players.forEach {
                    val player = it.audience
                    if (player.get(Identity.DISPLAY_NAME).isPresent) {
                        val playerUuid = player.get(Identity.UUID).get()
                        val playerDisplayName = player.get(Identity.DISPLAY_NAME).get() as TextComponent
                        if (voteData.voteStrings[playerUuid] != null) {
                            if (result.children().isNotEmpty()) result.append(Component.newline())
                            result.append(playerDisplayName)
                                .append(Component.text(" 的投票紀錄: "))
                                .append(Component.text(voteData.voteStrings[playerUuid]!!).color(voteStringColor))
                        } else if (option != "@a") {
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

    @Suppress("SpellCheckingInspection")
    private fun <C : CommonAudience<S>, S> voteset(ctx: CommandContext<C>, allPlayers: CommonAudience<S>) {
        var value = ctx.get<String>("value")
        if (value[0] != '+') value = "+$value"

        val target = ctx.optional<CommonPlayers<C>>("target")
        if (target.isEmpty) {
            onPlayerVote(ctx.sender().audience, ctx.sender().audience, value, allPlayers)
        } else {
            target.get().players.forEach { player ->
                onPlayerVote(ctx.sender().audience, player.audience, value, allPlayers)
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun <C : CommonAudience<S>, S> vytchat(ctx: CommandContext<C>, allPlayers: CommonAudience<S>) {
        // val command = ctx.rawInput[1]
        val command = ctx.rawInput().input().split(" ")[1]
        val videoid = ctx.optional<String>("videoid")

        val ytChatPrefix = Component.text("[YTChat] ").color(NamedTextColor.RED)
        when (command) {
            "start" -> {
                val videoId = videoid.get().replace(YoutubeFetcher.WATCH_URI, "")
                if (ytChatLooper == null || !ytChatLooper!!.isAlive) {
                    ytChatLooper = Thread {
                        ytFetcher = YoutubeFetcher(allPlayers, videoId)
                        ytFetcher.fetch()
                    }
                    ytChatLooper!!.start()
                    allPlayers.sendMessage(
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
                } else {
                    ctx.sender().sendMessage(
                        ytChatPrefix.append(Component.text("尚未啟動!"))
                    )
                }
            }
            "url", "urlall" -> {
                if (ytChatLooper != null && ytChatLooper!!.isAlive) {
                    val url = "${YoutubeFetcher.WATCH_URI}${ytFetcher.liveId}"
                    val urlMessage = ytChatPrefix
                        .append(Component.text("直播網址: "))
                        .append(Component.text(url).clickEvent(ClickEvent.openUrl(url)))
                    if (!command.endsWith("all")) {
                        ctx.sender().sendMessage(urlMessage)
                    } else {
                        allPlayers.sendMessage(urlMessage)
                    }
                } else {
                    ctx.sender().sendMessage(
                        ytChatPrefix.append(Component.text("尚未啟動!"))
                    )
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

    fun <S> onPlayerMessage(player: Audience, message: String, allPlayers: CommonAudience<S>) {
        if (player.get(Identity.UUID).isPresent) {
            val index = player.get(Identity.UUID).get()
            if (message.contains("+")) {
                if (message.substring(message.indexOf("+")).length >= 2 && voteReset) {
                    onPlayerVote(player, player, message, allPlayers)
                }
            }
        }
    }

    fun onPlayerDetectSpam(player: Audience, message: String): Boolean {
        if (player.get(Identity.UUID).isPresent) {
            val index = player.get(Identity.UUID).get()
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
