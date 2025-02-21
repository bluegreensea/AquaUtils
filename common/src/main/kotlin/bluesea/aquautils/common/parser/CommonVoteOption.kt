package bluesea.aquautils.common.parser

import bluesea.aquautils.common.CommonAudience

open class CommonVoteOption<C : CommonAudience<*>>(val option: String, open val players: ArrayList<C>?)
