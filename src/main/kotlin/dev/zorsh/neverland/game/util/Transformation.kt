package dev.zorsh.neverland.game.util

import net.minestom.server.coordinate.Vec

data class Transformation(
    val translation: Vec,
    val leftRotation: Quaternion,
    val scale: Vec,
    val rightRotation: Quaternion
)
