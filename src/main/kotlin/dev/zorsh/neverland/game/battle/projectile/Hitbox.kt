package dev.zorsh.neverland.game.battle.projectile

interface Hitbox {
    fun isIn(px: Float, py: Float, soulRadius: Float): Boolean
}