package dev.zorsh.neverland.game.battle

import dev.zorsh.neverland.game.battle.enemy.DeltaruneEnemy
import dev.zorsh.neverland.game.battle.player.DeltarunePlayer
import dev.zorsh.neverland.game.battle.projectile.ProjectileData
import dev.zorsh.neverland.game.ui.PlayerUICanvas
import java.util.UUID

interface INeverlandBattle {
    fun startBattle(onEnded: () -> Unit)
    fun endBattle()
    fun destroyBattle()

    fun isActive(): Boolean

    fun setUUID(uuid: UUID)

    fun getBattleInitialPlayers(): List<DeltarunePlayer>
    fun getBattlePlayers(): List<DeltarunePlayer>
    fun getBattleEnemies(): List<DeltaruneEnemy>

    fun getBBLocation(): Pair<Double, Double>

    fun createProjectile(px: Double, py: Double, projectileData: ProjectileData, afterCreated: (PlayerUICanvas, String, (Int) -> Unit) -> Unit)

    fun removePlayer(dPlayer: DeltarunePlayer)
}