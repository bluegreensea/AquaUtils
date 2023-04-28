package bluesea.aquautils.common

import bluesea.aquautils.fetcher.YoutubeFetcher
import cloud.commandframework.CommandManager
import cloud.commandframework.arguments.standard.BooleanArgument
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.context.CommandContext
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object CommandController {
    val LOGGER: Logger = LoggerFactory.getLogger("AquaUtils")
    private val lastMessage = HashMap<Component, String>()
    private val lastTimes = HashMap<Component, Int>()
    private var voteReset = false
    private val voteStrings = HashMap<Component, String>()
    private val optionStrings = ArrayList<String>(9)
    private var kick = true
    private lateinit var voteTextComponent: Component
    var ytChatLooper: Thread? = null

    fun <M : CommandManager<C>, C : CommonAudience<S>, S> register(manager: M, allPlayers: Audience) {
        manager.command(
            manager.commandBuilder("aquautils")
                .argument(StringArgument.optional("command"))
                .argument(BooleanArgument.optional("switch"))
                .handler {
                    aquautils(it)
                }
        )

        manager.command(
            manager.commandBuilder("vytchat")
                .permission("aquautils.vytchat")
                .argument(
                    StringArgument.builder<C>("command")
                        .single()
                        .withSuggestionsProvider { _, _ ->
                            listOf("start", "stop")
                        }
                )
                .argument(StringArgument.optional("videoid"))
                .handler {
                    vytchat(it, allPlayers)
                }
        )
    }

    private fun <C : CommonAudience<S>, S> aquautils(ctx: CommandContext<C>) {
        val command = ctx.getOptional<String>("command")
        val switch = ctx.getOptional<Boolean>("switch")

        if (command.isEmpty) {
            ctx.sender.sendMessage(Component.text("version: " + "\${version}"))
        } else if (ctx.hasPermission("aquautils.command")) {
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

    private fun <C : CommonAudience<S>, S> vytchat(ctx: CommandContext<C>, allPlayers: Audience) {
        val command = ctx.get<String>("command")
        val videoid = ctx.getOptional<String>("videoid")

        when (command) {
            "start" -> {
                if (ytChatLooper == null || !ytChatLooper!!.isAlive) {
                    ytChatLooper = Thread {
                        YoutubeFetcher(
                            allPlayers,
                            videoid.get()
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
        }
    }
}
