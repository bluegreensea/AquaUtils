package bluesea.aquautils.common

import bluesea.aquautils.common.parser.CommonPlayers
import bluesea.aquautils.common.parser.CommonVoteOption
import org.incendo.cloud.parser.ParserDescriptor

interface CommonParsersProvider<
    S,
    C : CommonAudience<S>,
    L : CommonPlayers<C>,
    O : CommonVoteOption<C>
    > {
    fun playersParser(): ParserDescriptor<C, L>

    fun voteOptionParser(vararg otherArgs: String): ParserDescriptor<C, O>
}
