package bluesea.aquautils.common.parser

import bluesea.aquautils.common.CommonAudience
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider

abstract class CommonPlayersParser<C : CommonAudience<*>, T : CommonPlayers<C>> : ArgumentParser<C, T>, BlockingSuggestionProvider<C>
