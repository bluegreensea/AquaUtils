package bluesea.aquautils.common

import java.util.UUID
import net.kyori.adventure.text.TextComponent

data class VoteData(
    val voteStrings: HashMap<UUID, String>,
    val optionStrings: ArrayList<String>,
    var winnerOption: TextComponent.Builder,
    var result: TextComponent.Builder,
    var resultForOther: TextComponent.Builder
)
