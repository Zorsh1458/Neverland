package dev.zorsh.neverland.game.network

import dev.zorsh.neverland.game.util.Transformation
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player

class FakeTextDisplay(
    entityId: Int,
    var text: Component,
    transformation: Transformation,
    teleportDuration: Int,
    interpolationDuration: Int,
    players: List<Player>,
    holder: MutableSet<FakeDisplay>? = null,
    var opacity: Byte,
) : FakeDisplay(
    entityId,
    transformation,
    teleportDuration,
    interpolationDuration,
    players,
    holder
) {
    fun changeText(newText: Component, playerOverride: List<Player>? = null) {
        if (playerOverride?.isNotEmpty() ?: true) {
            PacketManager.setTextDisplayMetadata(
                entityId,
                newText,
                transformation,
                playerOverride ?: players,
                interpolationDuration,
                teleportDuration,
                opacity
            )
            if (playerOverride == null) {
                text = newText
            }
        }
    }

    override fun changeTransformation(newTransformation: Transformation, newText: Component, newOpacity: Byte) {
        var new = newText
        if (newText == Component.text("___DEFAULT_TEXT___")) {
            new = text
        }
        PacketManager.setTextDisplayMetadata(
            entityId,
            new,
            newTransformation,
            players,
            interpolationDuration,
            teleportDuration,
            newOpacity
        )
        transformation = newTransformation
        opacity = newOpacity
        text = new
    }

    fun changeOnlyTransformation(newTransformation: Transformation, playerOverride: List<Player>? = null) {
        if (playerOverride?.isNotEmpty() ?: true) {
            PacketManager.setTransformation(
                entityId,
                newTransformation,
                playerOverride ?: players,
                interpolationDuration,
                teleportDuration
            )
            if (playerOverride == null) {
                transformation = newTransformation
            }
        }
    }
}