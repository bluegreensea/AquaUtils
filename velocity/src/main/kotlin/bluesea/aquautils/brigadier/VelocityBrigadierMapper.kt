package bluesea.aquautils.brigadier

import bluesea.aquautils.VelocityAudience
import bluesea.aquautils.arguments.PlayersArgumentType
import bluesea.aquautils.common.parser.CommonCommandOptionsParser
import bluesea.aquautils.parser.VelocityPlayersParser
import bluesea.aquautils.parser.VelocityVoteOptionParser
import com.mojang.brigadier.arguments.StringArgumentType
import io.leangen.geantyref.TypeToken
import org.incendo.cloud.velocity.VelocityCommandManager

class VelocityBrigadierMapper(private val velocityCommandManager: VelocityCommandManager<VelocityAudience>) {
    fun registerMappings() {
        val cloudBrigadierManager = velocityCommandManager.brigadierManager()

        val playersType = TypeToken.get(VelocityPlayersParser::class.java)
        cloudBrigadierManager.registerMapping(playersType) { builder ->
            builder.toConstant(PlayersArgumentType.players()).cloudSuggestions()
        }

        val voteOptionType = TypeToken.get(VelocityVoteOptionParser::class.java)
        cloudBrigadierManager.registerMapping(voteOptionType) { builder ->
            builder.toConstant(PlayersArgumentType.players()).cloudSuggestions()
        }

        val voteHistoryType = TypeToken.get(CommonCommandOptionsParser<VelocityAudience>()::class.java)
        cloudBrigadierManager.registerMapping(voteHistoryType) { builder ->
            builder.toConstant(StringArgumentType.greedyString()).cloudSuggestions()
        }
    }
}
