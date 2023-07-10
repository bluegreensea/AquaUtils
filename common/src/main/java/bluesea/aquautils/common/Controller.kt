package bluesea.aquautils.common

import bluesea.aquautils.common.Constants.MOD_ID
import bluesea.aquautils.fetcher.YoutubeFetcher
import cloud.commandframework.CommandManager
import cloud.commandframework.arguments.standard.BooleanArgument
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.context.CommandContext
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import java.util.UUID
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Controller {
    val LOGGER: Logger = LoggerFactory.getLogger(Constants.MOD_NAME)
    private val lastMessage = HashMap<UUID, String>()
    private val lastTimes = HashMap<UUID, Int>()
    private var voteReset = false
    private val voteStrings = HashMap<UUID, String>()
    private val optionStrings = ArrayList<String>(9)
    var kick = true
    private lateinit var voteTextComponent: Component
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
                .argument(StringArgument.of("command"))
                .argument(BooleanArgument.optional("switch"))
                .handler {
                    aquautils(it)
                }
        )

        manager.command(
            manager.commandBuilder("votereset")
                .permission("$MOD_ID.votereset")
                .argument(StringArgument.optional("options", StringArgument.StringMode.GREEDY))
                .handler {
                    votereset(it, provider.getAllPlayersAudience(it.sender.source))
                }
        )

        manager.command(
            manager.commandBuilder("voteget")
                .argument(
                    StringArgument.builder<C>("target").greedy().asOptional()
                        .withSuggestionsProvider { c, s ->
                            suggestionsFilteredPlayer(s, provider.getAllPlayersAudience(c.sender.source))
                        }
                )
                .handler {
                    voteget(it, provider.getAllPlayersAudience(it.sender.source))
                }
        )

        manager.command(
            manager.commandBuilder("voteset")
                .permission("$MOD_ID.voteset")
                .argument(StringArgument.of("value"))
                .argument(
                    StringArgument.builder<C>("target").greedy().asOptional()
                        .withSuggestionsProvider { c, s ->
                            suggestionsFilteredPlayer(s, provider.getAllPlayersAudience(c.sender.source))
                        }
                )
                .handler {
                    voteset(it, provider.getAllPlayersAudience(it.sender.source))
                }
        )

        manager.command(
            manager.commandBuilder("vytchat")
                .permission("$MOD_ID.vytchat")
                .literal("start")
                // .argument(StringArgument.of("videoid"))
                .argument(StringArgument.greedy("videoid"))
                .handler {
                    vytchat(it, provider.getAllPlayersAudience(it.sender.source))
                }
        ).command(
            manager.commandBuilder("vytchat")
                .permission("$MOD_ID.vytchat")
                .literal("stop")
                .handler {
                    vytchat(it, provider.getAllPlayersAudience(it.sender.source))
                }
        ).command(
            manager.commandBuilder("vytchat")
                .literal("url")
                .handler {
                    vytchat(it, provider.getAllPlayersAudience(it.sender.source))
                }
        ).command(
            manager.commandBuilder("vytchat")
                .permission("$MOD_ID.vytchat")
                .literal("urlall")
                .handler {
                    vytchat(it, provider.getAllPlayersAudience(it.sender.source))
                }
        )
    }

    private fun <C : CommonAudience<S>, S> aquautils(ctx: CommandContext<C>) {
        val command = ctx.getOptional<String>("command")
        val switch = ctx.getOptional<Boolean>("switch")

        if (command.isEmpty) {
            ctx.sender.sendMessage(Component.text("version: ${Constants.MOD_VERSION}"))
        } else if (ctx.hasPermission("$MOD_ID.command")) {
            if (switch.isEmpty) {
                ctx.sender.sendMessage(
                    Component.text("Aqua Utils kick: $kick")
                )
            } else {
                kick = switch.get()
                ctx.sender.sendMessage(
                    Component.text("Aqua Utils set kick: $kick")
                )
                LOGGER.info("set kick: $kick")
            }
        }
    }

    private fun <C : CommonAudience<S>, S> votereset(ctx: CommandContext<C>, allPlayers: CommonAudience<S>) {
        val options = ctx.getOptional<String>("options")

        if (options.isEmpty) {
            if (::voteTextComponent.isInitialized) {
                voteStrings.clear()

                val players = allPlayers.audience
                players.sendMessage(voteTextComponent)
            } else {
                ctx.sender.sendMessage(
                    Component.text("需要至少重置一次")
                )
            }
        } else {
            var textComponent = Component.empty()
            optionStrings.clear()
            var i = 0
            for (option in options.get().split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                i++
                textComponent = textComponent
                    .append(Component.text("+$i").color(NamedTextColor.YELLOW))
                    .append(Component.space())
                    .append(Component.text("$option "))
                optionStrings.add(option)
                if (i >= 9) {
                    break
                }
            }
            voteStrings.clear()
            voteReset = true
            val players = allPlayers.audience
            voteTextComponent = if (ctx.sender.audience.get(Identity.DISPLAY_NAME).isPresent) {
                Component.translatable(
                    "chat.type.text",
                    ctx.sender.audience.get(Identity.DISPLAY_NAME).get(),
                    textComponent
                )
            } else {
                Component.text("[Server]").append(textComponent)
            }
            players.sendMessage(voteTextComponent)
        }
    }

    private fun <C : CommonAudience<S>, S> voteget(ctx: CommandContext<C>, allPlayers: CommonAudience<S>) {
        val target = ctx.getOptional<String>("target")

        if (target.isEmpty) {
            var resultTextComponent = Component.empty()
            var notIntResultTextComponent = Component.empty()
            val allVotes = HashMap<String, Int>()
            voteStrings.forEach { (uuid, optionStr) ->
                try {
                    optionStr.substring(1).toInt()
                } catch (e: Exception) {
                    allPlayers.audience.forEachAudience { player ->
                        if (player.get(Identity.UUID).isPresent && player.get(Identity.UUID).get() == uuid) {
                            notIntResultTextComponent = notIntResultTextComponent
                                .append(Component.newline())
                                .append(player.get(Identity.DISPLAY_NAME).get())
                                .append(Component.text(" 投了 ${optionStr.substring(1)}"))
                        }
                    }
                    return@forEach
                }
                allVotes[optionStr] = allVotes.getOrDefault(optionStr, 0) + 1
            }
            var firstOptionVotes = 0
            var firstOptionStr = StringBuilder().append("無記錄")
            for ((optionStr, optionVotes) in allVotes) {
                if (optionStr.substring(1).toInt() > 0 &&
                    optionStr.substring(1).toInt() - 1 < optionStrings.size
                ) {
                    resultTextComponent = resultTextComponent
                        .append(Component.newline())
                        .append(Component.text(optionStr).color(NamedTextColor.YELLOW).appendSpace())
                        .append(Component.text(optionStrings[optionStr.substring(1).toInt() - 1]))
                        .append(Component.text(" 票數: $optionVotes"))
                    if (firstOptionVotes == optionVotes) {
                        firstOptionStr.append(", ").append(optionStrings[optionStr.substring(1).toInt() - 1])
                    } else if (optionVotes >= firstOptionVotes) {
                        firstOptionStr = StringBuilder(optionStrings[optionStr.substring(1).toInt() - 1])
                        firstOptionVotes = optionVotes
                    }
                }
            }

            resultTextComponent = Component
                .text("投票結果: $firstOptionStr")
                .append(resultTextComponent)
                .append(notIntResultTextComponent)
            ctx.sender.sendMessage(resultTextComponent)
        } else {
            val strings = StringBuilder()
            val filteredPlayer = getFilteredPlayer(ctx, allPlayers)
            filteredPlayer.forEachAudience { player: Audience ->
                if (player.get(Identity.DISPLAY_NAME).isPresent) {
                    val playerUuid = player.get(Identity.UUID).get()
                    val playerDisplayName = player.get(Identity.DISPLAY_NAME).get() as TextComponent
                    if (strings.toString() != "") strings.append("\n")
                    if (voteStrings[playerUuid] != null) {
                        strings.append(playerDisplayName.content()).append(" 的投票記錄: ")
                            .append(voteStrings[playerUuid])
                    } else {
                        strings.append(playerDisplayName.content()).append(" 的投票記錄: ")
                            .append("無記錄")
                    }
                }
            }
            ctx.sender.sendMessage(
                Component.text(if (strings.toString() == "") "無任何投票記錄" else strings.toString())
            )
        }
    }

    private fun <C : CommonAudience<S>, S> voteset(ctx: CommandContext<C>, allPlayers: CommonAudience<S>) {
        val target = ctx.getOptional<String>("target")
        var value = ctx.get<String>("value")
        if (value[0] != '+') value = "+$value"

        if (target.isEmpty) {
            onPlayerVote(ctx.sender.audience, ctx.sender.audience, value, allPlayers)
        } else {
            val filteredPlayer = getFilteredPlayer(ctx, allPlayers)
            filteredPlayer.forEachAudience { player: Audience ->
                onPlayerVote(ctx.sender.audience, player, value, allPlayers)
            }
        }
    }

    private fun <C : CommonAudience<S>, S> vytchat(ctx: CommandContext<C>, allPlayers: CommonAudience<S>) {
        val command = ctx.rawInput[1]
        val videoid = ctx.getOptional<String>("videoid")

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
                    allPlayers.sendMessage(
                        Component.empty()
                            .append(Component.text("[YTChat] ").color(NamedTextColor.RED))
                            .append(Component.text("啟動成功!"))
                    )
                    LOGGER.info("YTChat 啟動成功!")
                } else {
                    ctx.sender.sendMessage(
                        Component.empty()
                            .append(Component.text("[YTChat] ").color(NamedTextColor.RED))
                            .append(Component.text("啟動失敗－已經啟動!"))
                    )
                }
            }
            "stop" -> {
                if (ytChatLooper != null && ytChatLooper!!.isAlive) {
                    ytChatLooper!!.interrupt()
                    LOGGER.info("YTChat 已關閉!")
                } else {
                    ctx.sender.sendMessage(
                        Component.empty()
                            .append(Component.text("[YTChat] ").color(NamedTextColor.RED))
                            .append(Component.text("尚未啟動!"))
                    )
                }
            }
            "url", "urlall" -> {
                if (ytChatLooper != null && ytChatLooper!!.isAlive && ::videoId.isInitialized) {
                    val url = "https://www.youtube.com/watch?v=$videoId"
                    val urlMessage = Component.text("直播網址: ").append(
                        Component.text(url).clickEvent(ClickEvent.openUrl(url))
                    )
                    if (!command.contains("all")) {
                        ctx.sender.sendMessage(urlMessage)
                    } else {
                        allPlayers.sendMessage(urlMessage)
                    }
                } else {
                    ctx.sender.sendMessage(
                        Component.empty()
                            .append(Component.text("[YTChat] ").color(NamedTextColor.RED))
                            .append(Component.text("尚未啟動!"))
                    )
                }
            }
        }
    }

    private fun <S> suggestionsFilteredPlayer(input: String, allPlayers: CommonAudience<S>): List<String> {
        val completions = arrayListOf<String>()
        if ("@a".contains(input.lowercase())) {
            completions.add("@a")
        }
        allPlayers.audience.forEachAudience { player: Audience ->
            player.get(Identity.NAME).ifPresent { name: String ->
                if (name.lowercase().contains(input.lowercase())) {
                    completions.add(name)
                }
            }
        }
        return completions
    }

    private fun <S> getFilteredPlayer(ctx: CommandContext<*>, allPlayers: CommonAudience<S>): Audience {
        if (ctx.get<String>("target") == "@a") {
            return allPlayers.audience
        } else {
            return allPlayers.audience.filterAudience { player: Audience ->
                if (player.get(Identity.NAME).isPresent) {
                    return@filterAudience player.get(Identity.NAME).get().lowercase() == ctx.get<String>("target").lowercase()
                }
                false
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

            if (voteStrings[index] == null || voteStrings[index] != value) {
                voteStrings[index] = value

                if (sender == player) {
                    sender.sendMessage(Component.text("已將自己的投票改變為 $value"))
                } else {
                    if (player.get(Identity.DISPLAY_NAME).isPresent) {
                        sender.sendMessage(
                            Component
                                .text("已將 ")
                                .append(player.get(Identity.DISPLAY_NAME).get())
                                .append(Component.text(" 的投票設定為 $value"))
                        )
                    }
                }

                val buf = Unpooled.copiedBuffer("Test", CharsetUtil.UTF_8)
                allPlayers.sendPluginMessage("test", buf)
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
