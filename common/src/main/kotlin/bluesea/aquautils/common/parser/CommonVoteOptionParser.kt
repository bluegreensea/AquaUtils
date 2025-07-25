package bluesea.aquautils.common.parser

import bluesea.aquautils.common.CommonAudience
import bluesea.aquautils.common.Controller
import com.mojang.brigadier.Message
import kotlin.jvm.optionals.getOrNull
import net.kyori.adventure.text.Component
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion

abstract class CommonVoteOptionParser<C : CommonAudience<*>, T : CommonVoteOption<C>, P : CommonPlayersParser<C, L>, L : CommonPlayers<C>>(
    private val playersParser: P,
    private vararg val otherArgs: String
) : ArgumentParser<C, T>, BlockingSuggestionProvider<C> {
    abstract fun voteOption(input: String, players: List<C>?): T
    abstract fun tooltipMessage(message: Component): Message

    override fun parse(
        commandContext: CommandContext<C>,
        commandInput: CommandInput
    ): ArgumentParseResult<T> {
        val playersResult = playersParser.parse(commandContext, commandInput.copy())
        val input = commandInput.readString()
        val players = playersResult.parsedValue().getOrNull()?.players
        return ArgumentParseResult.success(voteOption(input, players))
    }

    override fun suggestions(
        commandContext: CommandContext<C>,
        commandInput: CommandInput
    ): Iterable<Suggestion> {
        val suggestions = arrayListOf<Suggestion>()
        suggestions.addAll(
            playersParser.suggestions(commandContext, commandInput.copy())
        )
        Controller.voteData.options.forEachIndexed { index, optionString ->
            suggestions.add(
                TooltipSuggestion.suggestion(
                    "+${(index + 1).toString(Controller.VOTE_OPTIONS_RADIX)}",
                    tooltipMessage(Component.text(optionString))
                )
            )
        }
        return suggestions
    }
}
