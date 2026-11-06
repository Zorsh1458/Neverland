package dev.zorsh.neverland.game.battle.player

import dev.zorsh.neverland.game.battle.enemy.DeltaruneEnemy

class PlayerBattleDataStorage(
    var optionsObjectNamesList: MutableList<String> = mutableListOf(),
    var optionsListSize: Pair<Int, Int> = 0 to 0,
    var optionsSelectorPosition: Pair<Int, Int> = 0 to 0,

    var selectedEnemyIndex: Int = 0,
    var selectedEnemy: DeltaruneEnemy? = null,

    var optionsShown: Boolean = false,
    var isAttacking: Boolean = false
)