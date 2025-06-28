package bluesea.aquautils

import bluesea.aquautils.common.CommonParsersProvider
import bluesea.aquautils.parser.VelocityPlayers
import bluesea.aquautils.parser.VelocityPlayersParser
import bluesea.aquautils.parser.VelocityVoteOption
import bluesea.aquautils.parser.VelocityVoteOptionParser
import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.parser.ParserDescriptor

class VelocityParsersProvider : CommonParsersProvider<
    CommandSource,
    VelocityAudience,
    VelocityPlayers,
    VelocityVoteOption
    > {
    override fun playersParser(): ParserDescriptor<VelocityAudience, VelocityPlayers> {
        return VelocityPlayersParser.playersParser()
    }

    override fun voteOptionParser(vararg otherArgs: String): ParserDescriptor<VelocityAudience, VelocityVoteOption> {
        return VelocityVoteOptionParser.voteOptionParser(*otherArgs)
    }
}
