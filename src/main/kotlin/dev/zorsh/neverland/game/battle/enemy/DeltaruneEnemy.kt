package dev.zorsh.neverland.game.battle.enemy

import dev.zorsh.neverland.game.battle.INeverlandBattle
import net.kyori.adventure.text.Component

abstract class DeltaruneEnemy(
    val name: String,
    val maxHitpoints: Int,
    val encounterMessages: List<Component>
) {
    var hitpoints = maxHitpoints

    lateinit var myBattle: INeverlandBattle

    var isAlive = true

    open fun askBoxSize(): Pair<Double, Double> {
        return 60.0 to 60.0
    }

    abstract suspend fun attack(onAttackEnds: () -> Unit = {})

    open fun die() {
        isAlive = false
    }
}