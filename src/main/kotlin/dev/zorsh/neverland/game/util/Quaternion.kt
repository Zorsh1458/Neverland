package dev.zorsh.neverland.game.util

import net.minestom.server.entity.Metadata
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class Quaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    val metadataEntry: Metadata.Entry<FloatArray>
        get() = Metadata.Quaternion(listOf(x, y, z, w,).toFloatArray())

    operator fun times(b: Quaternion): Quaternion {
        return Quaternion(
            x * b.w + b.x * w + y * b.z - z * b.y,
            y * b.w + b.y * w + z * b.x - x * b.z,
            z * b.w + b.z * w + x * b.y - y * b.x,
            w * b.w - x * b.x - y * b.y - z * b.z
        )
    }

    fun angle(): Float = 2f * acos(w)

    companion object {
        fun fromAxisAngle(angle: Float, x: Float, y: Float, z: Float): Quaternion {
            return Quaternion(x * sin(angle / 2), y * sin(angle / 2), z * sin(angle / 2), cos(angle / 2))
        }
    }
}
