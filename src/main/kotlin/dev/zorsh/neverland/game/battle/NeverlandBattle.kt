package dev.zorsh.neverland.game.battle

import dev.zorsh.neverland.game.Neverland
import dev.zorsh.neverland.game.battle.enemy.DeltaruneEnemy
import dev.zorsh.neverland.game.battle.player.DeltarunePlayer
import dev.zorsh.neverland.game.battle.projectile.ProjectileData
import dev.zorsh.neverland.game.network.FakeDisplay
import dev.zorsh.neverland.game.network.FakeTextDisplay
import dev.zorsh.neverland.game.network.PacketManager
import dev.zorsh.neverland.game.ui.CanvasSprite
import dev.zorsh.neverland.game.ui.PlayerUICanvas
import dev.zorsh.neverland.game.ui.ShaderTextColor
import dev.zorsh.neverland.game.util.FakeDisplayData
import dev.zorsh.neverland.game.util.Quaternion
import dev.zorsh.neverland.game.util.Transformation
import dev.zorsh.neverland.game.util.fontText
import dev.zorsh.neverland.game.util.runLater
import dev.zorsh.neverland.game.util.runRepeating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title.Times
import net.kyori.adventure.title.Title.title
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import java.time.Duration
import java.util.UUID
import kotlin.math.ceil

class NeverlandBattle(val players: List<DeltarunePlayer>, val enemies: List<DeltaruneEnemy>) : INeverlandBattle {

    var active = false

    lateinit var battleUUID: UUID

    private var onEndedAction = {}

    private val battleCanvas = BattleCanvas(players.mapNotNull { it.player }, this)

    private val playersNotQuit = players.toMutableList()

    private var battleJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    private val shulkerHitboxes = mutableSetOf<Int>()
    private val spawnedEntities = mutableSetOf<FakeDisplay>()

    private var theSoul: FakeTextDisplay? = null
    private var soulGraze: FakeTextDisplay? = null

    object BattleLocation {
        val TEST = Pos(1141.0, -59.0, 71.0)
        val UNDER_STATION = Pos(952.0, 99.6, 1101.0)
    }

    private val battleCenterLocation = BattleLocation.TEST

    private var battleBoxLocation = 0.0 to 20.0

    override fun getBBLocation() = battleBoxLocation

    override fun isActive() = active

    private fun newHitboxEntity(
        loc: Pos,
        scale: Double,
        playerToShow: List<Player> = players.mapNotNull { it.player },
    ) {
        PacketManager.spawnHitbox(loc, scale, playerToShow) { anchor, shulker ->
            shulkerHitboxes += anchor
            shulkerHitboxes += shulker
        }
    }

    override fun destroyBattle() {
        active = false
        for (pl in players) {
            try {
                pl.freeFromBattle(battleUUID, true)
            } catch (_: Exception) {
            }
        }
        val toDestroy = spawnedEntities.toList()
        for (ent in toDestroy) {
            try {
                ent.destroy()
            } catch (_: Exception) {
            }
        }
        spawnedEntities.clear()
        if (battleJob?.isCancelled == false) {
            battleJob?.cancel()
        }
        for (ent in shulkerHitboxes) {
            try {
                PacketManager.removeEntity(ent, getBattlePlayers().mapNotNull { it.player })
            } catch (_: Exception) {
            }
        }
        shulkerHitboxes.clear()
        scope.cancel()
        battleCanvas.myCanvas.clear()
    }

    override fun setUUID(uuid: UUID) {
        battleUUID = uuid
    }

    override fun getBattleInitialPlayers() = players

    override fun getBattlePlayers() =
        playersNotQuit.filter { it.player?.isOnline == true && it.myBattleUUID == battleUUID }

    override fun getBattleEnemies() = enemies

    override fun removePlayer(dPlayer: DeltarunePlayer) {
        dPlayer.player?.let { player ->
            battleCanvas.myCanvas.removePlayer(player)
            spawnedEntities.forEach { ent ->
                ent.destroy(player)
            }
        }
        playersNotQuit -= dPlayer
    }

    override fun createProjectile(
        px: Double,
        py: Double,
        projectileData: ProjectileData,
        afterCreated: (PlayerUICanvas, String, (Int) -> Unit) -> Unit,
    ) {
        val projId = UUID.randomUUID()
        battleCanvas.myCanvas.drawSprite(
            px,
            py,
            0.0,
            0.0,
            48,
            projectileData.sprites.first(),
            ShaderTextColor.pure("#ffffff"),
            "projectile_$projId"
        ) {
            if (projectileData.framesPerSprite != null) {
                battleCanvas.animateSprite(
                    projectileData.sprites,
                    projectileData.framesPerSprite,
                    "projectile_$projId",
                    stoppingCondition = { return@animateSprite !isActive() || !battleCanvas.myCanvas.hasObject("projectile_$projId") }
                )
            }
            runLater(1) {
                battleCanvas.myCanvas.setScale(1.0, 1.0, "projectile_$projId")
            }
            afterCreated(battleCanvas.myCanvas, "projectile_$projId") { baseDamageAmount ->
                for (pl in getBattlePlayers()) {
                    if (pl.noDamageTicks > 0) continue

                    var px = pl.player!!.position.x - battleCenterLocation.x
                    var py = pl.player!!.position.z - battleCenterLocation.z
                    px *= -1f
                    px *= 16f * 8f
                    py *= 16f * 8f
                    px += battleBoxLocation.first
                    py += battleBoxLocation.second
                    val projPosition = battleCanvas.myCanvas.getPosition("projectile_$projId")
                    px -= projPosition.first
                    py -= projPosition.second
                    if (projectileData.hitbox.isIn(px.toFloat(), py.toFloat(), 6f)) {
                        pl.damage(baseDamageAmount) { hp ->
                            battleCanvas.updateHealthInfo(hp, pl.maxhp, pl.uuid)
                            if (hp <= 0) {
                                pl.freeFromBattle(battleUUID, false)
                            }
                        }
                        animateSoulDamage(pl)
                        battleCanvas.animateStatusText(
                            -200.0,
                            Neverland.random.nextDouble() * 128 - 64,
                            2.25,
                            2.0,
                            ShaderTextColor.pure("#ffffff"),
                            Component.text(baseDamageAmount).style(Style.style(TextDecoration.BOLD)),
                            pl.player
                        )
                    } else if (projectileData.hitbox.isIn(px.toFloat(), py.toFloat(), 20f)) {
                        if (pl.tpGain == 0) {
                            soulGraze?.let { ent ->
                                val (sx, sy) = CanvasSprite.SOUL_OUTLINE.getSizeRatios()
                                ent.changeOnlyTransformation(
                                    Transformation(
                                        ent.transformation.translation,
                                        ent.transformation.leftRotation,
                                        Vec(2.5 * sx / 8, 2.5 * sy / 8, 1.0),
                                        ent.transformation.rightRotation
                                    ),
                                    listOf(pl.player!!)
                                )
                            }
                        }
                        pl.tpGain()
                        runLater(4) {
                            if (pl.tpGain == 0) {
                                soulGraze?.let { ent ->
                                    ent.changeOnlyTransformation(
                                        Transformation(
                                            ent.transformation.translation,
                                            ent.transformation.leftRotation,
                                            Vec(0.0, 0.0, 1.0),
                                            ent.transformation.rightRotation
                                        ),
                                        listOf(pl.player!!)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun animateSoulDamage(dPlayer: DeltarunePlayer) {
        val mcPlayer = dPlayer.player ?: return
        val soul = theSoul ?: return
        val frames = dPlayer.noDamageTicks
        var counter = 0
        val text = soul.text
        val color = text.color() ?: TextColor.color(255, 255, 255)
        val darkened = TextColor.color(color.red() / 2, color.green() / 2, color.blue() / 2)
        soul.changeText(text.color(darkened), listOf(mcPlayer))
        runRepeating(frames) { i ->
            counter = (counter + 1) % 2
            val power = frames - i - 1
            val shift = power * (counter * 2 - 1) * 1.75
            val offset = Vec(shift / 16 / 8, 0.0, 0.0)
            val transformation = soul.transformation
            theSoul?.changeOnlyTransformation(
                Transformation(
                    transformation.translation.add(offset),
                    transformation.leftRotation,
                    transformation.scale,
                    transformation.rightRotation
                ),
                listOf(mcPlayer)
            )
        }
        runLater(frames / 2) {
            soul.changeText(soul.text, listOf(mcPlayer))
        }
    }

    override fun startBattle(onEnded: () -> Unit) {
        onEndedAction = onEnded

        for (pl in players) {
            pl.updatePlayer()
        }

        for (pl in getBattlePlayers()) {
            pl.player?.showTitle(
                title(
                    fontText("\uD701", "#000000", "space:default"),
                    Component.text(""),
                    Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ZERO)
                )
            )
            runLater(3) {
                if (pl.player != null) {
                    MinecraftServer.getCommandManager().dispatcher.execute(
                        MinecraftServer.getCommandManager().consoleSender,
                        "sendshaderdata ${pl.player!!.name} 1"
                    )
                }
            }
            pl.lockInBattle(battleCenterLocation)
            pl.player?.playSound(Sound.sound(Key.key("encounter"), Sound.Source.MASTER, 1f, 1f))
            runLater(20) {
                pl.player?.playSound(Sound.sound(Key.key("weaponpull"), Sound.Source.MASTER, 1f, 1f))
            }
            runLater(30) {
                pl.player?.playSound(Sound.sound(Key.key("battle"), Sound.Source.MASTER, 1f, 1f))
            }
        }

        active = true

        enemies.forEach { enemy ->
            enemy.myBattle = this
        }

        runLater(2) {
            prepareBattle()
            battleCanvas.initCanvas()
            battleCanvas.setupLayout()
        }

        runLater(40) {
            CoroutineScope(Dispatchers.IO).launch {
                val job = scope.launch {
                    repeat(2) {
                        if (getBattlePlayers().isEmpty()) {
                            return@launch
                        }

                        battleCanvas.showPlayersOptions()
                        getBattlePlayers().forEach { dPlayer ->
                            battleCanvas.setTurnTimeScale(1.0, dPlayer.uuid)
                            dPlayer.handlePickingOption(battleCanvas)
                        }
                        val playerTurnLength = 300
                        runRepeating(playerTurnLength) { i ->
                            getBattlePlayers().forEach { dPlayer ->
                                battleCanvas.setTurnTimeScale(1.0 - (i + 1.0) / playerTurnLength, dPlayer.uuid)
                            }
                        }
                        delay((playerTurnLength + 5) / 20 * 1000L)
                        getBattlePlayers().forEach { dPlayer ->
                            dPlayer.stopPickingOption()
                        }
                        battleCanvas.hidePlayersOptions()
                        battleBoxOpen()
                        delay(500)
                        unlockSouls()
                        val jobs = mutableListOf<Job>()
                        for (enemy in enemies) {
                            jobs += launch {
                                enemy.attack()
                            }
                        }
                        jobs.joinAll()
                        delay(200)
                        lockSouls()
                        battleBoxClose()
                        delay(500)
                    }
                }
                battleJob = job
                job.join()
                endBattle()
            }
        }
    }

    override fun endBattle() {
        destroyBattle()
        onEndedAction()
    }

    private fun prepareBattle() {
        val loc = battleCenterLocation.withYaw(0f).withPitch(-90f)

        runLater(3) {
            // SOUL
            val sprite = CanvasSprite.SOUL
            PacketManager.spawnTextDisplay(
                loc,
                sprite.toTextValue().color(ShaderTextColor.pure("#ff0000").value),
                getBattlePlayers().mapNotNull { it.player },
                data = FakeDisplayData(
                    Transformation(
                        Vec(battleBoxLocation.first / 16 / 8, battleBoxLocation.second / 16 / 8, 0.0),
                        Quaternion(0f, 0f, 0f, 1f),
                        Vec(0.0, 0.0, 1.0),
                        Quaternion(0f, 0f, 0f, 1f)
                    ),
                    opacity = 251.toByte()
                ),
                seeThrough = false,
                alignment = TextDisplayMeta.Alignment.CENTER,
                lineWidth = 1000,
                isShadowed = false
            ) { ent ->
                spawnedEntities += ent
                theSoul = ent
            }

            // SOUL_GRAZE_OUTLINE
            val spriteGraze = CanvasSprite.SOUL_OUTLINE
            PacketManager.spawnTextDisplay(
                loc,
                spriteGraze.toTextValue().color(ShaderTextColor.effect(1, 6, 0).value),
                getBattlePlayers().mapNotNull { it.player },
                data = FakeDisplayData(
                    Transformation(
                        Vec(battleBoxLocation.first / 16 / 8, battleBoxLocation.second / 16 / 8, 0.0),
                        Quaternion(0f, 0f, 0f, 1f),
                        Vec(0.0, 0.0, 1.0),
                        Quaternion(0f, 0f, 0f, 1f)
                    ),
                    interpolationDuration = 0,
                    opacity = 251.toByte()
                ),
                seeThrough = false,
                alignment = TextDisplayMeta.Alignment.CENTER,
                lineWidth = 1000,
                isShadowed = false
            ) { ent ->
                spawnedEntities += ent
                soulGraze = ent
            }
        }
    }

    private fun unlockSouls() {
        val soul = theSoul ?: return
        val scaling = CanvasSprite.SOUL.getSizeRatios()
        theSoul?.changeOnlyTransformation(
            Transformation(
                soul.transformation.translation,
                soul.transformation.leftRotation,
                Vec(2.5 * scaling.first / 8, 2.5 * scaling.second / 8, 1.0),
                soul.transformation.rightRotation
            )
        )
        getBattlePlayers().forEach { battlePlayer ->
            battlePlayer.unlockSoul(
                battleBoxLocation,
                battleCanvas,
                getBattlePlayers().mapNotNull { dp -> dp.player }.filter { pl -> pl.uuid != battlePlayer.uuid })
        }
    }

    private fun lockSouls() {
        val soul = theSoul ?: return
        theSoul?.changeOnlyTransformation(
            Transformation(
                soul.transformation.translation,
                soul.transformation.leftRotation,
                Vec(0.0, 0.0, 1.0),
                soul.transformation.rightRotation
            )
        )
        getBattlePlayers().forEach {
            it.lockSoul()
        }
    }

    private fun battleBoxOpen() {
        val availableSizes = enemies.map { it.askBoxSize() }
        var sizeX = 60.0
        var sizeZ = 60.0
        if (availableSizes.isNotEmpty()) {
            val rand = availableSizes.toSet().random()
            sizeX = rand.first
            sizeZ = rand.second
        }
        battleCanvas.openBattleBox(0.0, 20.0, sizeX, sizeZ)
        val scale = 1 / 16.0 / 8.0
        val playerWidth = 0.3
        val soulWidth = 9.0 * scale
        val shulkScaleZ = ceil(sizeX * scale)
        val shulkScaleX = ceil(sizeZ * scale)
        val loc1 =
            battleCenterLocation.add(0.0, 0.0, sizeZ * scale + shulkScaleZ * 0.5 + playerWidth - soulWidth)
        newHitboxEntity(loc1, shulkScaleZ)
        val loc2 =
            battleCenterLocation.sub(0.0, 0.0, sizeZ * scale + shulkScaleZ * 0.5 + playerWidth - soulWidth)
        newHitboxEntity(loc2, shulkScaleZ)
        val loc3 =
            battleCenterLocation.add(sizeX * scale + shulkScaleX * 0.5 + playerWidth - soulWidth, 0.0, 0.0)
        newHitboxEntity(loc3, shulkScaleX)
        val loc4 =
            battleCenterLocation.sub(sizeX * scale + shulkScaleX * 0.5 + playerWidth - soulWidth, 0.0, 0.0)
        newHitboxEntity(loc4, shulkScaleX)
    }

    private fun battleBoxClose() {
        for (id in shulkerHitboxes) {
            PacketManager.removeEntity(id, players.mapNotNull { it.player })
        }
        shulkerHitboxes.clear()
        battleCanvas.closeBattleBox()
    }
}