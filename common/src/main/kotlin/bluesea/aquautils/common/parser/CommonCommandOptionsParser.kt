package bluesea.aquautils.common.parser

import bluesea.aquautils.common.CommonAudience
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.parser.standard.StringParser.StringMode
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider

class CommonCommandOptionsParser<C : CommonAudience<*>>(
    private vararg val args: String
) : ArgumentParser<C, String>, BlockingSuggestionProvider<C> {
    companion object {
        fun <C : CommonAudience<*>> voteHistoryParser(vararg args: String): ParserDescriptor<C, String> {
            return ParserDescriptor.of(CommonCommandOptionsParser(*args), String::class.java)
        }
    }

    override fun parse(
        commandContext: CommandContext<C>,
        commandInput: CommandInput
    ): ArgumentParseResult<String> {
        val stringParser = StringParser.stringParser<C>(StringMode.GREEDY).parser()
        return stringParser.parse(commandContext, commandInput)
    }

    override fun suggestions(
        commandContext: CommandContext<C>,
        commandInput: CommandInput
    ): Iterable<Suggestion> {
        val suggestions = arrayListOf<String>()
        val optionsString = commandInput.remainingInput()
        val options = if (optionsString.trim().isNotEmpty()) {
            optionsString.trim().split(" ")
        } else {
            emptyList()
        }
        // (Controller.voteHistory + otherArgs).forEach {
        // Controller.voteHistory.forEach {
        args.forEach {
            if (!optionsString.endsWith(" ")) {
                if (options.isEmpty()) {
                    suggestions.add(it)
                } else if (it.startsWith(options.last())) {
                    val option = it.substring(options.last().length)
                    if (option.isNotEmpty()) {
                        suggestions.add("$optionsString$option")
                    }
                }
            } else {
                if (!options.contains(it)) {
                    suggestions.add("$optionsString$it")
                }
            }
        }
        return SuggestionProvider
            .suggestingStrings<C>(suggestions)
            .suggestionsFuture(commandContext, commandInput)
            .get()
    }
}
