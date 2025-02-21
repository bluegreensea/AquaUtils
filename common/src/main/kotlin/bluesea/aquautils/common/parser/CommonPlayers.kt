package bluesea.aquautils.common.parser

import bluesea.aquautils.common.CommonAudience

open class CommonPlayers<C : CommonAudience<*>>(val players: ArrayList<C>)
