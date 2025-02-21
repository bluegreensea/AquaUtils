package bluesea.aquautils.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext

class PlayersArgumentType(val flags: Int) : ArgumentType<PlayersArgumentType.Players> {
    companion object {
        private const val SINGLE = 1
        private const val PLAYERS_ONLY = 2

        fun players(): PlayersArgumentType {
            return PlayersArgumentType(PLAYERS_ONLY)
        }

        fun getPlayers(context: CommandContext<*>, name: String): Players {
            return context.getArgument(name, Players::class.java)
        }

        fun isAllowedInPlayersString(c: Char): Boolean {
            return StringReader.isAllowedInUnquotedString(c) || c == '@'
        }
    }

    override fun parse(reader: StringReader): Players {
        return Players(reader.readPlayersString())
    }

    fun StringReader.readPlayersString(): String {
        val start = cursor
        while (canRead() && isAllowedInPlayersString(peek())) {
            skip()
        }
        return string.substring(start, cursor)
    }

    class Players(val playersString: String)
}
