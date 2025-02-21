package bluesea.aquautils.parser

import bluesea.aquautils.VelocityAudience
import bluesea.aquautils.common.parser.CommonPlayersParser
import com.velocitypowered.api.command.VelocityBrigadierMessage
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion
import org.incendo.cloud.caption.CaptionVariable
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.exception.parsing.ParserException
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.velocity.VelocityCaptionKeys

class VelocityPlayersParser : CommonPlayersParser<VelocityAudience, VelocityPlayers>() {
    companion object {
        fun playersParser(): ParserDescriptor<VelocityAudience, VelocityPlayers> {
            return ParserDescriptor.of(VelocityPlayersParser(), VelocityPlayers::class.java)
        }
    }

    override fun parse(
        commandContext: CommandContext<VelocityAudience>,
        commandInput: CommandInput
    ): ArgumentParseResult<VelocityPlayers> {
        val input = commandInput.readString()
        val players = arrayListOf<VelocityAudience>()
        val sender = commandContext.sender()
        val allPlayers = commandContext.get<ProxyServer>("ProxyServer").allPlayers
        when (input) {
            "@a" -> players.addAll(allPlayers.map { VelocityAudience(it) })
            "@s" -> players.add(sender)
            else -> commandContext.get<ProxyServer>("ProxyServer")
                .getPlayer(input)
                .orElse(null)?.let { player ->
                    players.add(VelocityAudience(player))
                }
        }
        if (players.isEmpty()) {
            return ArgumentParseResult.failure(
                PlayersParseException(
                    input,
                    commandContext
                )
            )
        }
        return ArgumentParseResult.success(VelocityPlayers(players))
    }

    override fun suggestions(
        commandContext: CommandContext<VelocityAudience>,
        commandInput: CommandInput
    ): Iterable<Suggestion> {
        val playersSuggestions = arrayListOf<Suggestion>(
            TooltipSuggestion.suggestion("@a", VelocityBrigadierMessage.tooltip(Component.translatable("argument.entity.selector.allPlayers"))),
            TooltipSuggestion.suggestion("@s", VelocityBrigadierMessage.tooltip(Component.translatable("argument.entity.selector.self")))
        )
        playersSuggestions.addAll(
            commandContext.get<ProxyServer>("ProxyServer").allPlayers.map { player -> Suggestion.suggestion(player.username) }
        )
        return playersSuggestions
    }

    class PlayersParseException internal constructor(
        input: String,
        context: CommandContext<*>
    ) : ParserException(
        VelocityPlayersParser::class.java,
        context,
        VelocityCaptionKeys.ARGUMENT_PARSE_FAILURE_PLAYER,
        CaptionVariable.of("input", input)
    )
}
