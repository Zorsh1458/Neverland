package dev.zorsh.neverland.game.battle.projectile

import dev.zorsh.neverland.game.ui.CanvasSprite

data class ProjectileData(
    val sprites: List<CanvasSprite>,
    val framesPerSprite: Int?,
    val hitbox: Hitbox
)