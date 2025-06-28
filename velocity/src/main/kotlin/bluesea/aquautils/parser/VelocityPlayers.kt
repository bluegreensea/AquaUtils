package bluesea.aquautils.parser

import bluesea.aquautils.VelocityAudience
import bluesea.aquautils.common.parser.CommonPlayers
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer

class VelocityPlayers(players: List<VelocityAudience>) : CommonPlayers<VelocityAudience>(players) {
    companion object {
        fun allPlayers(allPlayers: Collection<Player>): VelocityPlayers {
            return VelocityPlayers(allPlayers.map { VelocityAudience(it, it) })
        }

        fun allPlayers(proxyServer: ProxyServer): VelocityPlayers {
            return allPlayers(proxyServer.allPlayers)
        }
    }
}
