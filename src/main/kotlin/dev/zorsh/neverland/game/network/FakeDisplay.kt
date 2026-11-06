package dev.zorsh.neverland.game.network

import dev.zorsh.neverland.game.util.Transformation
import dev.zorsh.neverland.game.util.runLater
import dev.zorsh.neverland.game.util.runSync
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player

abstract class FakeDisplay(
    val entityId: Int,
    var transformation: Transformation,
    protected val teleportDuration: Int,
    protected val interpolationDuration: Int,
    var players: List<Player>,
    var holder: MutableSet<FakeDisplay>? = null,
) {

    var exists = true

    open fun destroy() {
        if (exists) {
            exists = false

            holder?.remove(this)
            runSync {
                PacketManager.removeEntity(entityId, players)
            }
            repeat(10) { i ->
                runLater(i * 20 + 20) {
                    PacketManager.removeEntity(entityId, players)
                }
            }
            runLater(400) {
                PacketManager.removeEntity(entityId, players)
            }
        }
    }

    open fun destroy(player: Player) {
        players = players - player
        repeat(10) { i ->
            runLater(i * 20) {
                PacketManager.removeEntity(entityId, listOf(player))
            }
        }
        runLater(400) {
            PacketManager.removeEntity(entityId, listOf(player))
        }
    }

    open fun changeTransformation(
        newTransformation: Transformation,
        newText: Component = Component.text("___DEFAULT_TEXT___"),
        newOpacity: Byte = 255.toByte(),
    ) {
        PacketManager.setTransformation(entityId, newTransformation, players, interpolationDuration, teleportDuration)
        transformation = newTransformation
    }
}