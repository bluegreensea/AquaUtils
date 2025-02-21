package bluesea.aquautils.parser

import bluesea.aquautils.VelocityAudience
import bluesea.aquautils.common.parser.CommonVoteOptionParser
import com.mojang.brigadier.Message
import com.velocitypowered.api.command.VelocityBrigadierMessage
import net.kyori.adventure.text.Component
import org.incendo.cloud.parser.ParserDescriptor

class VelocityVoteOptionParser : CommonVoteOptionParser<VelocityAudience, VelocityVoteOption, VelocityPlayersParser, VelocityPlayers>(
    VelocityPlayersParser()
) {
    companion object {
        fun voteOptionParser(): ParserDescriptor<VelocityAudience, VelocityVoteOption> {
            return ParserDescriptor.of(VelocityVoteOptionParser(), VelocityVoteOption::class.java)
        }
    }

    override fun voteOption(input: String, players: ArrayList<VelocityAudience>?): VelocityVoteOption {
        return VelocityVoteOption(input, players)
    }

    override fun tooltipMessage(message: Component): Message {
        return VelocityBrigadierMessage.tooltip(message)
    }
}
