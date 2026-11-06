package dev.zorsh.neverland.game.battle

import dev.zorsh.neverland.game.battle.enemy.SpritedEnemy
import dev.zorsh.neverland.game.battle.player.PlayerBattleDataStorage
import dev.zorsh.neverland.game.ui.CanvasSprite
import dev.zorsh.neverland.game.ui.PlayerUICanvas
import dev.zorsh.neverland.game.ui.ShaderTextColor
import dev.zorsh.neverland.game.util.runInfinite
import dev.zorsh.neverland.game.util.runLater
import dev.zorsh.neverland.game.util.runRepeating
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import java.util.UUID
import kotlin.math.max

class BattleCanvas(val players: List<Player>, val battle: INeverlandBattle) {
    val myCanvas = PlayerUICanvas()
    lateinit var playerBattleDataStorage: HashMap<UUID, PlayerBattleDataStorage>

    fun initCanvas() {
        myCanvas.initialize(players)
    }

    private fun section(name: String, active: Boolean, action: () -> Unit) {
        if (active) {
            action()
        }
    }

    fun closeBattleBox() {
        val scale = myCanvas.getScale("battle_box_inner")
        runRepeating(10) { t ->
            val i = 9 - t
            myCanvas.setScale(i * 0.1 * scale.first, i * 0.1 * scale.second, "battle_box_inner")
            myCanvas.setScale(i * 0.1 * scale.first + 2, i * 0.1 * scale.second + 2, "battle_box_outer")
            myCanvas.rotate(3.1415f / 10f, "battle_box_inner")
            myCanvas.rotate(3.1415f / 10f, "battle_box_outer")
        }
        runLater(12) {
            myCanvas.remove("battle_box_inner")
            myCanvas.remove("battle_box_outer")
        }
    }

    fun openBattleBox(px: Double, py: Double, sx: Double, sy: Double) {
        section("BATTLE_BOX", true) {
            myCanvas.drawSprite(
                px,
                py,
                0.0,
                0.0,
                60,
                CanvasSprite.SQUARE,
                ShaderTextColor.pure("#00c400"),
                "battle_box_outer"
            )

            myCanvas.drawSprite(
                px,
                py,
                0.0,
                0.0,
                59,
                CanvasSprite.SQUARE,
                ShaderTextColor.pure("#000000"),
                "battle_box_inner"
            )
        }

        runRepeating(10) { t ->
            val i = t + 1
            myCanvas.setScale(i * 0.1 * sx, i * 0.1 * sy, "battle_box_inner")
            myCanvas.setScale(i * 0.1 * sx + 2, i * 0.1 * sy + 2, "battle_box_outer")
            myCanvas.rotate(3.1415f / 10f, "battle_box_inner")
            myCanvas.rotate(3.1415f / 10f, "battle_box_outer")
        }
    }

    fun showPlayersOptions() {
        val dPlayers = battle.getBattlePlayers()
        dPlayers.mapNotNull { it.player?.uuid }.forEach { uuid ->
            showPlayerOptions(uuid)
        }
    }

    fun hidePlayersOptions() {
        val dPlayers = battle.getBattlePlayers()
        dPlayers.mapNotNull { it.player?.uuid }.forEach { uuid ->
            hidePlayerOptions(uuid)
        }
    }

    fun showPlayerOptions(playerUUID: UUID) {
        if (playerBattleDataStorage[playerUUID]?.optionsShown == true) return
        playerBattleDataStorage[playerUUID]?.optionsShown = true

        playerOptionsObjectNamesToLift.forEach { objName ->
            myCanvas.move(0.0, 36.0, objName, playerUUID)
        }
    }

    fun hidePlayerOptions(playerUUID: UUID) {
        if (playerBattleDataStorage[playerUUID]?.optionsShown == false) return
        playerBattleDataStorage[playerUUID]?.optionsShown = false

        playerOptionsObjectNamesToLift.forEach { objName ->
            myCanvas.move(0.0, -36.0, objName, playerUUID)
        }
    }

    fun animateSprite(
        spriteList: List<CanvasSprite>,
        framesPerSprite: Int,
        objName: String,
        stoppingCondition: () -> Boolean,
    ) {
        runInfinite(1) { i, action ->
            if (stoppingCondition()) {
                action.cancel()
                return@runInfinite
            }

            val currentSprite = spriteList[(i / framesPerSprite) % spriteList.size]
            myCanvas.setSprite(
                currentSprite,
                ShaderTextColor.pure("#ffffff"),
                objName
            )
        }
    }

    fun updateHealthInfo(hp: Int, maxHp: Int, playerId: UUID) {
        val offsetPercentage = hp.toDouble() / maxHp.toDouble()
        val py = myCanvas.getPosition("player_hp_bar_green", playerId).second
        myCanvas.setPosition(35.0 + offsetPercentage * 28, py, "player_hp_bar_green", playerId)
        myCanvas.setScale(offsetPercentage * 28, 4.0, "player_hp_bar_green", playerId)
        val hpCountText = Component.text("                       \n$hp / $maxHp")
        myCanvas.setText(hpCountText, "player_hp_counter", playerId)
    }

    fun setDebugText(text: String) {
        battle.getBattlePlayers().forEach { dPlayer ->
            myCanvas.setText(Component.text("Debug: $text"), "debug_info", dPlayer.uuid)
        }
    }

    fun setTurnTimeScale(scale: Double, playerId: UUID) {
        myCanvas.setScale(scale * 98, 1.0, "selection_box_time_scale", playerId)
    }

    fun createEnemiesList(player: Player) {
        val storage = playerBattleDataStorage[player.uuid] ?: return

        val px = -128.0
        var py = -140.0

        storage.optionsObjectNamesList = mutableListOf()
        storage.optionsListSize = 1 to battle.getBattleEnemies().size
        battle.getBattleEnemies().forEach { enemy ->
            val objName = "enemy_list_entry_${UUID.randomUUID()}"
            val component = Component.text("                              \n⏵ ${enemy.name}")
            myCanvas.drawText(
                px, py,
                1.5, 1.5,
                16,
                component, ShaderTextColor.pure("#ffffff"),
                alignment = TextDisplayMeta.Alignment.LEFT,
                saveAs = objName,
                player = player
            ) {
                storage.optionsObjectNamesList.add(objName)
            }

            val objNameHpBg = "enemy_list_entry_hp_bg_${UUID.randomUUID()}"
            myCanvas.drawSprite(
                px + 256, py + 12,
                30.0, 4.0,
                16,
                CanvasSprite.SQUARE, ShaderTextColor.pure("#6b0e19"),
                objNameHpBg,
                player
            ) {
                storage.optionsObjectNamesList.add(objNameHpBg)
            }

            val objNameHp = "enemy_list_entry_hp_${UUID.randomUUID()}"
            myCanvas.drawSprite(
                px + 256 + 30 * (1.0 - enemy.hitpoints.toDouble() / enemy.maxHitpoints), py + 12,
                30.0 * enemy.hitpoints / enemy.maxHitpoints, 4.0,
                15,
                CanvasSprite.SQUARE, ShaderTextColor.pure("#1bf230"),
                objNameHp,
                player
            ) {
                storage.optionsObjectNamesList.add(objNameHp)
            }
            py -= 20
        }
    }

    fun clearEnemiesList(playerUUID: UUID) {
        val storage = playerBattleDataStorage[playerUUID] ?: return
        storage.optionsObjectNamesList.forEach { objName ->
            myCanvas.remove(objName, playerUUID)
        }
        storage.optionsObjectNamesList.clear()
    }

    fun createOptionsSelector(player: Player) {
        val storage = playerBattleDataStorage[player.uuid] ?: return
        storage.optionsSelectorPosition = 1 to 1
        myCanvas.drawSprite(
            -232.0, -133.0, 0.75, 0.75, 16,
            CanvasSprite.SOUL,
            ShaderTextColor.pure("#ff0000"),
            "menu_selector",
            player
        )
    }

    fun moveOptionsSelectorLeft(playerUUID: UUID) {
        val storage = playerBattleDataStorage[playerUUID] ?: return
        val pos = storage.optionsSelectorPosition
        if (pos.first <= 1) return
        storage.optionsSelectorPosition = pos.first - 1 to pos.second
        myCanvas.move(-200.0, 0.0, "menu_selector", playerUUID)
    }

    fun moveOptionsSelectorRight(playerUUID: UUID) {
        val storage = playerBattleDataStorage[playerUUID] ?: return
        val pos = storage.optionsSelectorPosition
        val size = storage.optionsListSize
        if (pos.first >= size.first) return
        storage.optionsSelectorPosition = pos.first + 1 to pos.second
        myCanvas.move(200.0, 0.0, "menu_selector", playerUUID)
    }

    fun moveOptionsSelectorUp(playerUUID: UUID) {
        val storage = playerBattleDataStorage[playerUUID] ?: return
        val pos = storage.optionsSelectorPosition
        if (pos.second <= 1) return
        storage.optionsSelectorPosition = pos.first to pos.second - 1
        myCanvas.move(0.0, 20.0, "menu_selector", playerUUID)
    }

    fun moveOptionsSelectorDown(playerUUID: UUID) {
        val storage = playerBattleDataStorage[playerUUID] ?: return
        val pos = storage.optionsSelectorPosition
        val size = storage.optionsListSize
        if (pos.second >= size.second) return
        storage.optionsSelectorPosition = pos.first to pos.second + 1
        myCanvas.move(0.0, -20.0, "menu_selector", playerUUID)
    }

    fun removeOptionsSelector(playerUUID: UUID) {
        myCanvas.remove("menu_selector", playerUUID)
    }

    fun setPlayerButtonSelection(objName: String, playerUUID: UUID) {
        val col = ShaderTextColor.pure("#ffffff")
        when (objName) {
            "player_button_fight" -> myCanvas.setSprite(CanvasSprite.DBUTTON_FIGHT_SELECTED, col, objName, playerUUID)
            "player_button_act" -> myCanvas.setSprite(CanvasSprite.DBUTTON_ACT_SELECTED, col, objName, playerUUID)
            "player_button_item" -> myCanvas.setSprite(CanvasSprite.DBUTTON_ITEM_SELECTED, col, objName, playerUUID)
            "player_button_mercy" -> myCanvas.setSprite(CanvasSprite.DBUTTON_MERCY_SELECTED, col, objName, playerUUID)
            "player_button_defend" -> myCanvas.setSprite(CanvasSprite.DBUTTON_DEFEND_SELECTED, col, objName, playerUUID)
        }
    }

    fun removePlayerButtonSelection(objName: String, playerUUID: UUID) {
        val col = ShaderTextColor.pure("#ffffff")
        when (objName) {
            "player_button_fight" -> myCanvas.setSprite(CanvasSprite.DBUTTON_FIGHT, col, objName, playerUUID)
            "player_button_act" -> myCanvas.setSprite(CanvasSprite.DBUTTON_ACT, col, objName, playerUUID)
            "player_button_item" -> myCanvas.setSprite(CanvasSprite.DBUTTON_ITEM, col, objName, playerUUID)
            "player_button_mercy" -> myCanvas.setSprite(CanvasSprite.DBUTTON_MERCY, col, objName, playerUUID)
            "player_button_defend" -> myCanvas.setSprite(CanvasSprite.DBUTTON_DEFEND, col, objName, playerUUID)
        }
    }

    fun startAttackUI(player: Player) {
        val storage = playerBattleDataStorage[player.uuid]
        storage?.let { st ->
            st.selectedEnemy = battle.getBattleEnemies()[st.optionsSelectorPosition.second - 1]
            st.selectedEnemyIndex = st.optionsSelectorPosition.second - 1
        }

        myCanvas.drawSprite(
            -128.0, -132.0,
            72.0, 16.0,
            24,
            CanvasSprite.SQUARE,
            ShaderTextColor.pure("#38090c"),
            "attack_box_outer",
            player
        )
        myCanvas.drawSprite(
            -126.0, -132.0,
            72.0, 14.0,
            23,
            CanvasSprite.SQUARE,
            ShaderTextColor.pure("#000000"),
            "attack_box_inner",
            player
        )
        myCanvas.drawSprite(
            -195.0, -132.0,
            5.0, 16.0,
            22,
            CanvasSprite.SQUARE,
            ShaderTextColor.pure("#ffff00"),
            "attack_box_crit_zone_outer",
            player
        )
        myCanvas.drawSprite(
            -195.0, -132.0,
            3.0, 14.0,
            21,
            CanvasSprite.SQUARE,
            ShaderTextColor.pure("#000000"),
            "attack_box_crit_zone_inner",
            player
        )
        myCanvas.drawSprite(
            6.0, -132.0,
            3.0, 14.0,
            20,
            CanvasSprite.SQUARE,
            ShaderTextColor.pure("#ffffff"),
            "attack_box_damage_indicator",
            player
        ) {
            playerBattleDataStorage[player.uuid]?.isAttacking = true
            runRepeating(22) { i, task ->
                if (playerBattleDataStorage[player.uuid]?.isAttacking == false) {
                    task.cancel()
                    return@runRepeating
                }

                myCanvas.move(-10.0, 0.0, "attack_box_damage_indicator", player.uuid)

                if (i % 2 == 1) {
                    val objName = "attack_box_damage_indicator_shadow_${UUID.randomUUID()}"
                    myCanvas.drawSprite(
                        6.0 + (0.75 - i) * 10, -132.0,
                        3.0, 14.0,
                        20,
                        CanvasSprite.SQUARE,
                        ShaderTextColor.pure("#ffffff"),
                        objName,
                        player
                    ) {
                        runRepeating(8) { t ->
                            val brightness = 1f - (t + 1) / 8f
                            myCanvas.setSprite(
                                CanvasSprite.SQUARE,
                                ShaderTextColor.pure(TextColor.color(brightness, brightness, brightness)),
                                objName,
                                player.uuid
                            )
                            myCanvas.setScale(
                                3.0 - 3.0 * t / 8.0,
                                14.0,
                                objName,
                                player.uuid
                            )
                        }
                        runLater(9) {
                            myCanvas.remove(objName, player.uuid)
                        }
                    }
                }
            }
            runLater(24) {
                val storage = playerBattleDataStorage[player.uuid] ?: return@runLater
                if (storage.isAttacking) {
                    val ind = storage.selectedEnemyIndex
                    val pos = myCanvas.getPosition("enemy_$ind")
                    animateStatusText(
                        pos.first, pos.second,
                        1.75, 1.75,
                        14,
                        ShaderTextColor.pure("#ff5050"),
                        Component.text("Промах"),
                        player
                    )
                }

                storage.isAttacking = false
                removeAttackUI(player.uuid)
            }
        }
    }

    fun confirmAttack(player: Player) {
        val storage = playerBattleDataStorage[player.uuid] ?: return
        if (!storage.isAttacking) return

        storage.isAttacking = false

        //// TODO("Remove hp calculation from BattleCanvas as it's not its responsibility")
        //// For testing purposes
        val enemy = storage.selectedEnemy
        enemy?.hitpoints = max(0, enemy.hitpoints - 10)
        ////

        val ind = storage.selectedEnemyIndex

        val frames = 10
        var counter = 0
        val pos = myCanvas.getPosition("enemy_$ind")
        animateStatusText(
            pos.first, pos.second,
            2.0, 2.0,
            14,
            ShaderTextColor.pure("#ff5050"),
            Component.text(10),
            player
        )
        runRepeating(frames) { i ->
            counter = (counter + 1) % 2
            val power = frames - i - 1
            val shift = power * (counter * 2 - 1) * 16f / frames
            myCanvas.setPosition(
                pos.first + shift, pos.second,
                "enemy_$ind"
            )
        }
    }

    fun removeAttackUI(playerUUID: UUID) {
        playerBattleDataStorage[playerUUID]?.isAttacking = false
        myCanvas.remove("attack_box_outer", playerUUID)
        myCanvas.remove("attack_box_inner", playerUUID)
        myCanvas.remove("attack_box_crit_zone_outer", playerUUID)
        myCanvas.remove("attack_box_crit_zone_inner", playerUUID)
        myCanvas.remove("attack_box_damage_indicator", playerUUID)
    }

    fun animateStatusText(
        px: Double,
        py: Double,
        sx: Double,
        sy: Double,
        color: ShaderTextColor,
        text: Component,
        player: Player? = null,
    ) {
        animateStatusText(px, py, sx, sy, 32, color, text, player)
    }

    fun animateStatusText(
        px: Double,
        py: Double,
        sx: Double,
        sy: Double,
        z: Int,
        color: ShaderTextColor,
        text: Component,
        player: Player? = null,
    ) {
        val objName = "status_text_${UUID.randomUUID()}"
        myCanvas.drawText(
            px, py, sx * 3, 0.0, z, text, color,
            saveAs = objName,
            player = player
        ) {
            var ySpeed = 4.0
            var yPos = 0.0
            runRepeating(20) { i ->
                val t = i + 1
                if (i < 3) {
                    myCanvas.setScale(sx * 3 - sx * 2 * t / 3, sy * t / 3, objName, player?.uuid)
                }
                ySpeed -= t
                yPos += ySpeed
                if (yPos <= 0.0) {
                    yPos = 0.0
                    ySpeed *= -0.75
                    ySpeed = 0.0
                }
                myCanvas.move(6.0 * (1.0 - i / 20.0), ySpeed, objName, player?.uuid)
            }
            runLater(30) {
                runRepeating(10) { i ->
                    val t = i + 1
                    myCanvas.setScale(sx - sx * t / 10, sy + sy * 0.5f * t / 10, objName, player?.uuid)
                    myCanvas.move(0.0, 2.0, objName, player?.uuid)
                }
            }
            runLater(52) {
                myCanvas.remove(objName, player?.uuid)
            }
        }
    }

    val playerOptionsObjectNamesToLift = mutableSetOf<String>()
    fun setupLayout() {
        playerBattleDataStorage = hashMapOf()
        playerOptionsObjectNamesToLift.clear()
        section("ENEMIES", true) {
            val spritedEnemies = battle.getBattleEnemies().filterIsInstance<SpritedEnemy>()
            for (enemy in spritedEnemies.withIndex()) {
                myCanvas.drawSprite(
                    320.0,
                    20.0 + (spritedEnemies.size - 1) * 48 - enemy.index * 72,
                    0.0,
                    0.0,
                    16,
                    enemy.value.canvasSprites.first(),
                    ShaderTextColor.pure("#ffffff"),
                    "enemy_${enemy.index}"
                ) {
                    runLater(10 + enemy.index) {
                        runRepeating(10) { t ->
                            val i = t + 1
                            myCanvas.setScale(i / 10.0, i / 10.0, "enemy_${enemy.index}")
                            myCanvas.move(-20.0 + i * 2.0, 0.0, "enemy_${enemy.index}")
                        }
                    }
                    animateSprite(
                        enemy.value.canvasSprites,
                        enemy.value.framesPerSprite,
                        "enemy_${enemy.index}",
                        stoppingCondition = { return@animateSprite !battle.isActive() || !enemy.value.isAlive }
                    )
                }
            }
        }

        section("BG_SLIDER", true) {
            myCanvas.drawSprite(
                790.0,
                0.0,
                800.0,
                800.0,
                -2,
                CanvasSprite.SQUARE,
                ShaderTextColor.pure("#000000"),
                "fg_slider_1"
            ) {
                runRepeating(30) { i ->
                    if (i > 19) {
                        val t = i - 19
                        myCanvas.move(160.0 - t * 16.0, 0.0, "fg_slider_1")
                    }
                }
                runLater(31) {
                    myCanvas.remove("fg_slider_1")
                }
            }

            myCanvas.drawSprite(
                -790.0,
                0.0,
                800.0,
                800.0,
                -2,
                CanvasSprite.SQUARE,
                ShaderTextColor.pure("#000000"),
                "fg_slider_2"
            ) {
                runRepeating(30) { i ->
                    if (i > 19) {
                        val t = i - 19
                        myCanvas.move(-160.0 + t * 16.0, 0.0, "fg_slider_2")
                    }
                }
                runLater(31) {
                    myCanvas.remove("fg_slider_2")
                }
            }

            myCanvas.drawSprite(
                790.0,
                0.0,
                801.0,
                800.0,
                -1,
                CanvasSprite.SQUARE,
                ShaderTextColor.pure("#ffffff"),
                "fg_slider_3"
            ) {
                runRepeating(30) { i ->
                    if (i > 19) {
                        val t = i - 19
                        myCanvas.move(160.0 - t * 16.0, 0.0, "fg_slider_3")
                    }
                }
                runLater(31) {
                    myCanvas.remove("fg_slider_3")
                }
            }

            myCanvas.drawSprite(
                -790.0,
                0.0,
                801.0,
                800.0,
                -1,
                CanvasSprite.SQUARE,
                ShaderTextColor.pure("#ffffff"),
                "fg_slider_4"
            ) {
                runRepeating(30) { i ->
                    if (i > 19) {
                        val t = i - 19
                        myCanvas.move(-160.0 + t * 16.0, 0.0, "fg_slider_4")
                    }
                }
                runLater(31) {
                    myCanvas.remove("fg_slider_4")
                }
            }
        }

        section("BATTLE_FIELD_BG", true) {
            myCanvas.drawSprite(
                0.0,
                0.0,
                800.0,
                800.0,
                128,
                CanvasSprite.SQUARE,
                ShaderTextColor.effect(1, 4, 0)
            )

            myCanvas.drawSprite(
                0.0,
                -478.0,
                800.0,
                400.0,
                64,
                CanvasSprite.SQUARE,
                ShaderTextColor.pure("#000000")
            )

            myCanvas.drawSprite(
                0.0,
                -514.0,
                800.0,
                400.0,
                32,
                CanvasSprite.SQUARE,
                ShaderTextColor.pure("#000000")
            )

            myCanvas.drawSprite(
                0.0,
                -78.0,
                800.0,
                1.0,
                32,
                CanvasSprite.SQUARE,
                ShaderTextColor.pure("#2e1e25")
            )

            myCanvas.drawSprite(
                0.0,
                -114.0,
                800.0,
                1.0,
                31,
                CanvasSprite.SQUARE,
                ShaderTextColor.pure("#2e1e25")
            )
        }

        section("PLAYER_STUFF", true) {
            val dPlayers = battle.getBattlePlayers()
            dPlayers.forEach { dPlayer ->
                playerBattleDataStorage[dPlayer.uuid] = PlayerBattleDataStorage()
                dPlayer.player?.let { actualPlayer ->
                    section("DEBUG", true) {
                        myCanvas.drawText(
                            0.0, 160.0, 2.0, 2.0, 48,
                            Component.text("Debug:"),
                            ShaderTextColor.pure("#ffffff"),
                            TextDisplayMeta.Alignment.CENTER,
                            1000,
                            "debug_info",
                            actualPlayer
                        )
                    }

                    section("SELECTION_BOX_DECORATION", true) {
                        myCanvas.drawSprite(
                            0.0,
                            -115.0,
                            100.0,
                            38.0,
                            62,
                            CanvasSprite.SQUARE,
                            ShaderTextColor.pure("#00ffff"),
                            "selection_box_outline",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "selection_box_outline"
                        }

                        myCanvas.drawSprite(
                            0.0,
                            -115.0,
                            98.0,
                            36.0,
                            61,
                            CanvasSprite.SQUARE,
                            ShaderTextColor.pure("#000000"),
                            "selection_box_inner",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "selection_box_inner"
                        }

                        myCanvas.drawSprite(
                            0.0,
                            -96.0,
                            98.0,
                            17.0,
                            59,
                            CanvasSprite.SQUARE,
                            ShaderTextColor.pure("#000000"),
                            "selection_box_inner2",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "selection_box_inner2"
                        }

                        runInfinite(10) { _, action ->
                            if (!battle.isActive() || !myCanvas.targetPlayers.contains(dPlayer.player)) {
                                action.cancel()
                                return@runInfinite
                            }

                            val objName1 = "player_box_decorative_animation_${UUID.randomUUID()}"
                            myCanvas.drawSprite(
                                -99.0,
                                -96.0,
                                1.0,
                                18.0,
                                60,
                                CanvasSprite.SQUARE,
                                ShaderTextColor.pure("#00ffff"),
                                objName1,
                                actualPlayer
                            ) {
                                runRepeating(40) { i ->
                                    myCanvas.move(0.5 + i / 60.0, 0.0, objName1, actualPlayer.uuid)
                                    val brightness = 1f - (i + 1) / 40f
                                    val col = TextColor.color(0f, brightness, brightness)
                                    myCanvas.setSprite(
                                        CanvasSprite.SQUARE,
                                        ShaderTextColor.pure(col),
                                        objName1,
                                        actualPlayer.uuid
                                    )
                                }
                                runLater(41) {
                                    myCanvas.remove(objName1, actualPlayer.uuid)
                                }
                            }

                            val objName2 = "player_box_decorative_animation_${UUID.randomUUID()}"
                            myCanvas.drawSprite(
                                99.0,
                                -96.0,
                                1.0,
                                18.0,
                                60,
                                CanvasSprite.SQUARE,
                                ShaderTextColor.pure("#00ffff"),
                                objName2,
                                actualPlayer
                            ) {
                                runRepeating(40) { i ->
                                    myCanvas.move(-0.5 - i / 60.0, 0.0, objName2, actualPlayer.uuid)
                                    val brightness = 1f - (i + 1) / 40f
                                    val col = TextColor.color(0f, brightness, brightness)
                                    myCanvas.setSprite(
                                        CanvasSprite.SQUARE,
                                        ShaderTextColor.pure(col),
                                        objName2,
                                        actualPlayer.uuid
                                    )
                                }
                                runLater(41) {
                                    myCanvas.remove(objName2, actualPlayer.uuid)
                                }
                            }
                        }

                        myCanvas.drawSprite(
                            0.0,
                            -78.0,
                            100.0,
                            1.0,
                            30,
                            CanvasSprite.SQUARE,
                            ShaderTextColor.pure("#00ffff"),
                            player = actualPlayer
                        )

                        myCanvas.drawSprite(
                            0.0,
                            -78.0,
                            98.0,
                            1.0,
                            29,
                            CanvasSprite.SQUARE,
                            ShaderTextColor.pure("#0a2847"),
                            player = actualPlayer
                        )

                        myCanvas.drawSprite(
                            0.0,
                            -78.0,
                            0.0,
                            1.0,
                            28,
                            CanvasSprite.SQUARE,
                            ShaderTextColor.pure("#3990ed"),
                            "selection_box_time_scale",
                            actualPlayer
                        )

                        myCanvas.drawSprite(
                            0.0,
                            -78.0,
                            10.0,
                            1.0,
                            27,
                            CanvasSprite.SQUARE,
                            ShaderTextColor.pure("#000000"),
                            player = actualPlayer
                        )

                        myCanvas.drawText(
                            0.0,
                            -83.0,
                            1.0,
                            1.0,
                            26,
                            Component.text("⌚"),
                            ShaderTextColor.pure("#d9d9d9"),
                            player = actualPlayer
                        )
                    }

                    section("PLAYER_NAME", true) {
                        myCanvas.drawText(
                            -30.0,
                            -93.0,
                            1.2,
                            1.2,
                            57,
                            Component.text("                           \n${actualPlayer.username}"),
                            ShaderTextColor.pure("#ffffff"),
                            TextDisplayMeta.Alignment.LEFT,
                            1000,
                            "player_name",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "player_name"
                        }

                        myCanvas.drawText(
                            -30.0 + 1.0,
                            -93.0 - 1.0,
                            1.2,
                            1.2,
                            58,
                            Component.text("                           \n${actualPlayer.username}"),
                            ShaderTextColor.pure("#444444"),
                            TextDisplayMeta.Alignment.LEFT,
                            1000,
                            "player_name_shadow",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "player_name_shadow"
                        }
                    }

                    section("HEALTH_BAR", true) {
                        myCanvas.drawText(
                            72.0,
                            -110.0,
                            1.2,
                            1.2,
                            58,
                            Component.text("                       \nHP"),
                            ShaderTextColor.pure("#ffffff"),
                            TextDisplayMeta.Alignment.LEFT,
                            1000,
                            "player_hp_text",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "player_hp_text"
                        }

                        myCanvas.drawSprite(
                            63.0,
                            -103.0,
                            28.0,
                            4.0,
                            58,
                            CanvasSprite.SQUARE,
                            ShaderTextColor.pure("#6b0e19"),
                            "player_hp_bar_red",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "player_hp_bar_red"
                        }

                        val offsetPercentage = dPlayer.hp.toDouble() / dPlayer.maxhp.toDouble()
                        myCanvas.drawSprite(
                            35.0 + offsetPercentage * 28,
                            -103.0,
                            offsetPercentage * 28,
                            4.0,
                            57,
                            CanvasSprite.SQUARE,
                            ShaderTextColor.pure("#1bf230"),
                            "player_hp_bar_green",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "player_hp_bar_green"
                        }

                        myCanvas.drawText(
                            36.0,
                            -97.0,
                            1.2,
                            1.2,
                            58,
                            Component.text("                       \n${dPlayer.hp} / ${dPlayer.maxhp}"),
                            ShaderTextColor.pure("#ffffff"),
                            TextDisplayMeta.Alignment.RIGHT,
                            1000,
                            "player_hp_counter",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "player_hp_counter"
                        }
                    }

                    section("BUTTONS", true) {
                        val buttonColor = ShaderTextColor.pure("#ffffff")

                        myCanvas.drawSprite(
                            -70.0,
                            -132.5,
                            0.5,
                            0.5,
                            57,
                            CanvasSprite.DBUTTON_FIGHT,
                            buttonColor,
                            "player_button_fight",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "player_button_fight"
                        }

                        myCanvas.drawSprite(
                            -35.0,
                            -132.5,
                            0.5,
                            0.5,
                            57,
                            CanvasSprite.DBUTTON_ACT,
                            buttonColor,
                            "player_button_act",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "player_button_act"
                        }

                        myCanvas.drawSprite(
                            0.0,
                            -132.5,
                            0.5,
                            0.5,
                            57,
                            CanvasSprite.DBUTTON_ITEM,
                            buttonColor,
                            "player_button_item",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "player_button_item"
                        }

                        myCanvas.drawSprite(
                            35.0,
                            -132.5,
                            0.5,
                            0.5,
                            57,
                            CanvasSprite.DBUTTON_MERCY,
                            buttonColor,
                            "player_button_mercy",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "player_button_mercy"
                        }

                        myCanvas.drawSprite(
                            70.0,
                            -132.5,
                            0.5,
                            0.5,
                            57,
                            CanvasSprite.DBUTTON_DEFEND,
                            buttonColor,
                            "player_button_defend",
                            actualPlayer
                        ) {
                            playerOptionsObjectNamesToLift += "player_button_defend"
                        }
                    }
                }
            }
        }
    }
}