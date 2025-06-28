package bluesea.aquautils.common

import bluesea.aquautils.common.Constants.ID
import bluesea.aquautils.common.parser.CommonCommandOptionsParser
import bluesea.aquautils.common.parser.CommonPlayers
import bluesea.aquautils.common.parser.CommonVoteOption
import bluesea.aquautils.fetcher.YoutubeFetcher
import java.util.Optional
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
import org.incendo.cloud.suggestion.FilteringSuggestionProcessor
import org.incendo.cloud.util.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Controller {
    val LOGGER: Logger = LoggerFactory.getLogger(Constants.NAME)
    private val lastMessage = HashMap<UUID, String>()
    private val lastTimes = HashMap<UUID, Int>()
    private const val SILENT_OPTION_STRING = "--silent"
    private const val LESS_OPTION_STRING = "--less"

    private val ignoredVoteChars = arrayOf(" ", "+")
    private val voteStringColor = NamedTextColor.YELLOW
    private val voteOptionColor = NamedTextColor.GREEN
    const val VOTE_OPTIONS_RADIX = 16
    private var voteReset = false
    val voteData = VoteData(
        playerVotes = HashMap(),
        options = ArrayList(VOTE_OPTIONS_RADIX - 1),
        winnerOption = Component.text("無紀錄"),
        winnerOptionVotes = 0,
        result = Component.text(),
        resultForOther = Component.text()
    )
    private var voteTextComponent = Component.text()

    val voteHistory = arrayListOf<String>()
    var kick = true
    var discord = "https://discord.gg/xxxxxxx"
    var lobbyServer = "lobby"
    var serversPanel = "servers"
    var serverIpService = "https://api.ipify.org"
    var fallbackServerIp = ""
    var fallbackServerPort = 25565
    var ytLiveIdRecord = ""

    var ytFetcher: YoutubeFetcher? = null
    val serverLinks = mutableMapOf<String, Pair<Component, String>>()

    fun init(console: CommonAudience<*>, provider: CommonAudienceProvider<*, *>) {
        initServerLinks()

        restoreVotes(console, provider.getAllPlayers())
        restoreYtChat(provider)
    }

    private fun initServerLinks() {
        serverLinks["dc"] = Pair(Component.text("DC 群"), discord)
    }

    private fun restoreVotes(console: CommonAudience<*>, allPlayers: CommonPlayers<*>) {
        if (voteData.options.isNotEmpty()) {
            voteCounting(Optional.of(console), allPlayers, silent = true)
            voteReset = true
        }
    }

    private fun restoreYtChat(provider: CommonAudienceProvider<*, *>) {
        if (ytLiveIdRecord.isNotBlank()) {
            ytFetcher = YoutubeFetcher(provider, ytLiveIdRecord, false)
            ytFetcher!!.chatLooper.start()
        }
    }

    fun getServerLinks(): List<Pair<Component, String>> {
        return serverLinks.values.toList()
    }

    fun <M : CommandManager<C>, C : CommonAudience<S>, S> register(manager: M, provider: CommonAudienceProvider<S, C>) {
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
                .optional(
                    "options",
                    CommonCommandOptionsParser.voteHistoryParser(*voteHistory.toTypedArray())
                    // CommonCommandOptionsParser.voteHistoryParser(Controller.voteHistory + SILENT_OPTION_STRING)
                )
                .handler {
                    votereset(it, Optional.of(provider.getConsoleServerAudience()), provider.getAllPlayers())
                }
        )

        manager.suggestionProcessor(
            FilteringSuggestionProcessor(
                FilteringSuggestionProcessor.Filter.startsWith<C>(true).and { _, s, i ->
                    StringUtils.trimBeforeLastSpace(s, i)
                }
            )
        )

        // manager.command(
        //     manager.commandBuilder("votehistory")
        //         .permission("$ID.votehistory")
        //         .optional("options", CommonVoteHistoryParser.voteHistoryParser())
        //         .handler {
        //             votehisoty(it, provider.getConsoleServerAudience(), provider.getAllPlayersAudience(it.sender()))
        //         }
        // )

        manager.command(
            manager.commandBuilder("voteget")
                .optional("voteoption", provider.parsersProvider().voteOptionParser(LESS_OPTION_STRING))
                // .optional("options", StringParser.stringParser())
                // { c, i ->
                //     val suggestions = arrayListOf<String>()
                //     if (!i.peekString().contains(LESS_OPTION_STRING)) {
                //         suggestions.add(LESS_OPTION_STRING)
                //     }
                //     SuggestionProvider
                //         .suggestingStrings<C>(suggestions)
                //         .suggestionsFuture(c, i)
                // }
                .handler {
                    voteget(it, provider.getAllPlayers())
                }
        )

        manager.command(
            manager.commandBuilder("voteset")
                .permission("$ID.voteset")
                .required("value", StringParser.stringParser())
                .optional("target", provider.parsersProvider().playersParser())
                .handler {
                    voteset(it, Optional.of(provider.getConsoleServerAudience()), provider.getAllPlayers())
                }
        )

        val vytchatBuilder = manager.commandBuilder("vytchat")
            .handler {
                vytchat(it, provider.getAllPlayers(), provider)
            }
        manager.command(
            vytchatBuilder
                .permission("$ID.vytchat")
                .literal("start")
                .required("videoid", StringParser.greedyStringParser())
        ).command(
            vytchatBuilder
                .permission("$ID.vytchat")
                .literal("start_continue")
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
    private fun <C : CommonAudience<*>> votereset(ctx: CommandContext<C>, console: Optional<CommonAudience<*>>, allPlayers: CommonPlayers<*>) {
        val options = ctx.optional<String>("options")

        if (options.isEmpty || options.get() == SILENT_OPTION_STRING) {
            if (voteData.options.isNotEmpty()) {
                voteDataReset(console, allPlayers)
                var sign = ""
                if (!options.isEmpty) {
                    sign = " (${SILENT_OPTION_STRING.substring(2)})}"
                }
                voteResetMessage(ctx.sender(), console, allPlayers, sign, !options.isEmpty)
            } else {
                ctx.sender().sendMessage(
                    Component.text("需要至少重置一次")
                )
            }
        } else {
            voteData.options.clear()
            val commandOptions = options.get().split(" ")
            var sign = ""
            val silent = commandOptions.contains(SILENT_OPTION_STRING)
            if (silent) {
                sign = " (${SILENT_OPTION_STRING.substring(2)})}"
            }
            var i = 0
            for (option in commandOptions) {
                if (option == SILENT_OPTION_STRING) {
                    continue
                }
                i++
                voteData.options.add(option)
                if (i >= VOTE_OPTIONS_RADIX - 1) {
                    break
                }
            }
            voteDataReset(console, allPlayers)
            voteData.options.forEach {
                if (!voteHistory.contains(it)) {
                    voteHistory.add(it)
                }
            }
            voteResetMessage(ctx.sender(), console, allPlayers, sign, silent)
        }
    }

    private fun voteResetMessage(
        sender: CommonAudience<*>,
        console: Optional<CommonAudience<*>>,
        allPlayers: CommonPlayers<*>,
        sign: String,
        silent: Boolean
    ) {
        val textComponent = Component.text()
        voteData.options.forEachIndexed { i, option ->
            textComponent
                .append(
                    Component.text("+${(i + 1).toString(VOTE_OPTIONS_RADIX).uppercase()}").color(voteStringColor)
                ).appendSpace()
                .append(Component.text(option).color(voteOptionColor)).appendSpace()
        }
        voteTextComponent = Component.text()
        if (sender.audience.get(Identity.DISPLAY_NAME).isPresent) {
            voteTextComponent.append(
                Component.translatable(
                    "chat.type.text",
                    sender.audience.get(Identity.DISPLAY_NAME).get().append(Component.text(sign)),
                    textComponent.build()
                )
            )
        } else {
            voteTextComponent.append(Component.text("[Server]")).append(Component.text(sign)).appendSpace()
                .append(textComponent.build())
        }
        console.ifPresent {
            it.sendMessage(
                sender.audience.get(Identity.DISPLAY_NAME).get()
                    .append(Component.text(sign))
                    .append(Component.text(":")).appendSpace()
                    .append(textComponent.build())
            )
        }
        if (!silent) {
            allPlayers.sendMessage(voteTextComponent.build())
        } else {
            sender.sendMessage(voteTextComponent.build())
        }
    }

    private fun voteDataReset(console: Optional<CommonAudience<*>>, allPlayers: CommonPlayers<*>) {
        voteData.playerVotes.clear()
        voteData.winnerOption = Component.text("無紀錄")
        voteData.winnerOptionVotes = 0
        voteCounting(console, allPlayers, silent = true)
    }

    // TODO: voteData network
    private fun voteCounting(
        console: Optional<CommonAudience<*>>,
        allPlayers: CommonPlayers<*>,
        index: UUID? = null,
        value: String? = null,
        silent: Boolean = false
    ) {
        if (index != null && value != null) {
        }
        val votesByOption = LinkedHashMap<String, Int>()
        for (votedInt in 0..<voteData.options.size) {
            val votedString = "+${(votedInt + 1).toString(VOTE_OPTIONS_RADIX).uppercase()}"
            votesByOption[votedString] = 0
        }
        voteData.result = Component.text()
        voteData.resultForOther = Component.text()
        voteData.playerVotes.forEach { (uuid, votedString) ->
            try {
                val voteValue = votedString.substring(1).toInt(VOTE_OPTIONS_RADIX)
                if (voteValue !in 1..voteData.options.size) {
                    throw NumberFormatException()
                }
            } catch (_: NumberFormatException) {
                allPlayers.forEachAudience { player ->
                    if (player.get(Identity.UUID).isPresent && player.get(Identity.UUID).get() == uuid) {
                        voteData.resultForOther.append(Component.newline())
                            .append(player.get(Identity.DISPLAY_NAME).get()).appendSpace()
                            .append(Component.text("投了")).appendSpace()
                            .append(Component.text(votedString).color(voteStringColor))
                    }
                }
                return@forEach
            }
            votesByOption.merge(votedString, 1, Int::plus)
        }
        voteData.winnerOptionVotes = 0
        var winnerOption = Component.text().content("無紀錄")
        votesByOption.forEach { (votedString, votes) ->
            try {
                votedString.substring(1).toInt(VOTE_OPTIONS_RADIX).takeIf { it in 1..voteData.options.size }?.let {
                    val optionString = voteData.options[it - 1]
                    voteData.result.append(Component.newline())
                        .append(Component.text(votedString).color(voteStringColor)).appendSpace()
                        .append(Component.text(optionString).color(voteOptionColor)).appendSpace()
                        .append(Component.text("票數:")).appendSpace()
                        .append(Component.text(votes))
                    if (votes > 0) {
                        if (votes == voteData.winnerOptionVotes) {
                            winnerOption.append(Component.text(",")).appendSpace()
                                .append(Component.text(optionString).color(voteOptionColor))
                        } else if (votes > voteData.winnerOptionVotes) {
                            winnerOption = Component.text()
                                .append(Component.text(optionString).color(voteOptionColor))
                            voteData.winnerOptionVotes = votes
                        }
                    }
                }
            } catch (e: StringIndexOutOfBoundsException) {
                LOGGER.error("", e)
            } catch (e: NumberFormatException) {
                LOGGER.error("", e)
            } catch (e: IllegalArgumentException) {
                LOGGER.error("", e)
            }
        }
        if (voteData.winnerOption.toString() != winnerOption.build().toString()) {
            voteData.winnerOption = winnerOption.build()
            if (!silent) {
                val msg = Component.text("投票結果已變更為:").appendSpace()
                    .append(voteData.winnerOption)
                console.ifPresent { it.sendMessage(msg) }
                allPlayers.sendMessage(msg)
            }
        }

        // val buf = Unpooled.copiedBuffer("Test", CharsetUtil.UTF_8)
        // allPlayers.sendPluginMessage("test", buf)
    }

    @Suppress("SpellCheckingInspection")
    private fun <C : CommonAudience<S>, S> voteget(ctx: CommandContext<C>, allPlayers: CommonPlayers<*>) {
        val voteOption = ctx.optional<CommonVoteOption<C>>("voteoption")

        if (voteOption.isEmpty) {
            ctx.sender().sendMessage(
                Component.text("投票結果:").appendSpace()
                    .append(voteData.winnerOption)
                    .append(voteData.result.build())
                    .append(voteData.resultForOther.build())
            )
        } else {
            val result = Component.text()
            val option = voteOption.get().option
            val players = voteOption.get().players
            if (players == null) {
                allPlayers.forEachAudience { player ->
                    if (player.get(Identity.DISPLAY_NAME).isPresent) {
                        val playerUuid = player.get(Identity.UUID).get()
                        val playerDisplayName = player.get(Identity.DISPLAY_NAME).get()
                        if (voteData.playerVotes.contains(playerUuid) && voteData.playerVotes[playerUuid] == option) {
                            if (result.content().isEmpty()) {
                                result.append(Component.text("投")).appendSpace()
                                    .append(Component.text(option).color(voteStringColor)).appendSpace()
                                    .append(Component.text("的玩家:")).appendSpace()
                                    .append(Component.newline())
                            } else {
                                result.append(Component.text(",")).appendSpace()
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
                        if (voteData.playerVotes[playerUuid] != null) {
                            if (result.children().isNotEmpty()) result.append(Component.newline())
                            result.append(playerDisplayName).appendSpace()
                                .append(Component.text("的投票紀錄:")).appendSpace()
                                .append(Component.text(voteData.playerVotes[playerUuid]!!).color(voteStringColor))
                        } else if (option != "@a") {
                            result.append(playerDisplayName).appendSpace()
                                .append(Component.text("的投票紀錄:")).appendSpace()
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
    private fun <C : CommonAudience<S>, S> voteset(
        ctx: CommandContext<C>,
        console: Optional<CommonAudience<*>>,
        allPlayers: CommonPlayers<*>
    ) {
        var value = ctx.get<String>("value")
        if (value[0] != '+') value = "+$value"

        val target = ctx.optional<CommonPlayers<C>>("target")
        if (target.isEmpty) {
            onPlayerVote(ctx.sender(), ctx.sender().audience, value, console, allPlayers, true)
        } else {
            target.get().players.forEach { player ->
                onPlayerVote(ctx.sender(), player.audience, value, console, allPlayers, true)
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun <C : CommonAudience<S>, S> vytchat(
        ctx: CommandContext<C>,
        allPlayers: CommonPlayers<*>,
        provider: CommonAudienceProvider<S, C>
    ) {
        val command = ctx.rawInput().input().split(" ")[1]
        val videoid = ctx.optional<String>("videoid")

        val ytChatPrefix = Component.text("[YTChat]").color(NamedTextColor.RED).appendSpace()
        when (command) {
            "start", "start_continue" -> {
                val videoId = videoid.get().replace(YoutubeFetcher.WATCH_URI, "")
                val sendInitMsgs = command != "start_continue"
                if (ytFetcher == null || !ytFetcher!!.chatLooper.isAlive) {
                    ytFetcher = YoutubeFetcher(provider, videoId, sendInitMsgs)
                    ytFetcher!!.chatLooper.start()
                    if (sendInitMsgs) {
                        allPlayers.sendMessage(
                            ytChatPrefix.append(Component.text("啟動成功!"))
                        )
                    }
                    LOGGER.info("YTChat 啟動成功!")
                } else {
                    ctx.sender().sendMessage(
                        ytChatPrefix.append(Component.text("啟動失敗－已經啟動!"))
                    )
                }
            }
            "stop" -> {
                if (ytFetcher != null && ytFetcher!!.chatLooper.isAlive) {
                    ytFetcher!!.chatLooper.interrupt()
                } else {
                    ctx.sender().sendMessage(
                        ytChatPrefix.append(Component.text("尚未啟動!"))
                    )
                }
            }
            "url", "urlall" -> {
                if (ytFetcher != null && ytFetcher!!.chatLooper.isAlive) {
                    val url = "${YoutubeFetcher.WATCH_URI}${ytFetcher!!.liveId}"
                    val urlMessage = ytChatPrefix
                        .append(Component.text("直播網址:")).appendSpace()
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

    private fun onPlayerVote(
        sender: CommonAudience<*>,
        player: Audience,
        message: String,
        console: Optional<CommonAudience<*>>,
        allPlayers: CommonPlayers<*>,
        silent: Boolean = false
    ) {
        if (player.get(Identity.UUID).isPresent) {
            val index = player.get(Identity.UUID).get()

            try {
                var value = message.substring(message.indexOf("+").let { it..it + 1 })
                if (value.toCharArray()[1] in '\ud800'..'\udfff') {
                    value = message.substring(message.indexOf("+").let { it..it + 2 })
                }

                var isIgnoredChar = false
                for (char in ignoredVoteChars) {
                    if (value == char) {
                        isIgnoredChar = true
                        break
                    }
                }
                try {
                    val voteValue = value.substring(1).toInt(VOTE_OPTIONS_RADIX)
                    if (voteValue in 1..voteData.options.size) {
                        value = value.uppercase()
                    }
                } catch (_: NumberFormatException) {}
                if (!isIgnoredChar && (voteData.playerVotes.contains(index) || voteData.playerVotes[index] != value)) {
                    voteData.playerVotes[index] = value

                    val voteNumber = Component.text(value).color(voteStringColor)
                    val msg = Component.text()
                    val playerDisplayName = player.get(Identity.DISPLAY_NAME)
                    if (sender.audience == player) {
                        msg.append(Component.text("已將自己的投票改變為").appendSpace().append(voteNumber))
                    } else {
                        if (playerDisplayName.isPresent) {
                            msg.append(Component.text("已將")).appendSpace()
                                .append(playerDisplayName.get()).appendSpace()
                                .append(Component.text("的投票設定為")).appendSpace()
                                .append(voteNumber)
                        }
                    }
                    sender.audience.get(Identity.DISPLAY_NAME).ifPresent {
                        if (playerDisplayName.isPresent && console.isPresent) {
                            console.get().sendMessage(
                                it.append(Component.text(":")).appendSpace().append(msg.build())
                            )
                        }
                    }
                    sender.sendMessage(msg.build())

                    voteCounting(console, allPlayers, index, value, silent)
                }
            } catch (e: StringIndexOutOfBoundsException) {
                LOGGER.error("", e)
            }
        }
    }

    fun onPlayerMessage(player: CommonAudience<*>, message: String, console: Optional<CommonAudience<*>>, allPlayers: CommonPlayers<*>) {
        if (message.contains("+")) {
            if (message.substring(message.indexOf("+")).length >= 2 && voteReset) {
                onPlayerVote(player, player.audience, message, console, allPlayers)
            }
        }
    }

    private fun onPlayerStateChange(player: Audience, console: Optional<CommonAudience<*>>, allPlayers: CommonPlayers<*>, leave: Boolean) {
        player.get(Identity.UUID).ifPresent { uuid ->
            if (voteData.playerVotes.containsKey(uuid)) {
                val value = if (leave) {
                    ""
                } else {
                    voteData.playerVotes[uuid]
                }
                voteCounting(console, allPlayers, uuid, value)
            }
        }
    }

    fun onPlayerLogin(player: Audience, console: Optional<CommonAudience<*>>, allPlayers: CommonPlayers<*>) {
        onPlayerStateChange(player, console, allPlayers, false)
    }

    fun onPlayerDisconnect(player: Audience, console: Optional<CommonAudience<*>>, allPlayers: CommonPlayers<*>) {
        onPlayerStateChange(player, console, allPlayers, true)
    }

    fun onPlayerDetectSpam(player: Audience, message: String): Boolean {
        var kickPlayer = false
        player.get(Identity.UUID).ifPresent { uuid ->
            if (message != lastMessage.getOrDefault(uuid, "")) {
                lastMessage[uuid] = message
                lastTimes.remove(uuid)
            } else {
                lastTimes.merge(uuid, 1, Int::plus)
                if (lastTimes[uuid]!! >= 2) {
                    if (kick) {
                        lastTimes.remove(uuid)
                        kickPlayer = true
                    }
                }
            }
        }
        return kickPlayer
    }
}
