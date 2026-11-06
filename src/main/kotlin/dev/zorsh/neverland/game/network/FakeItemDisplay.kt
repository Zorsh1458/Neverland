package dev.zorsh.neverland.game.network

import dev.zorsh.neverland.game.util.Transformation
import net.minestom.server.entity.Player

class FakeItemDisplay(
    entityId: Int,
    transformation: Transformation,
    teleportDuration: Int,
    interpolationDuration: Int,
    players: List<Player>,
    holder: MutableSet<FakeDisplay>? = null
) : FakeDisplay(
    entityId,
    transformation,
    teleportDuration,
    interpolationDuration,
    players,
    holder
)