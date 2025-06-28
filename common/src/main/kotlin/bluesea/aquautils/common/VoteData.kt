package bluesea.aquautils.common

import java.util.UUID
import net.kyori.adventure.text.TextComponent

data class VoteData(
    val playerVotes: HashMap<UUID, String>,
    val options: ArrayList<String>,
    var winnerOption: TextComponent,
    var winnerOptionVotes: Int,
    var result: TextComponent.Builder,
    var resultForOther: TextComponent.Builder
)
