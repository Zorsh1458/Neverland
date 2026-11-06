package dev.zorsh.neverland.game.battle.player

import dev.zorsh.neverland.game.battle.BattleCanvas
import dev.zorsh.neverland.game.battle.BattleManager
import dev.zorsh.neverland.game.util.InputHolder
import dev.zorsh.neverland.game.util.fontText
import dev.zorsh.neverland.game.util.hideFromEveryone
import dev.zorsh.neverland.game.util.runInfinite
import dev.zorsh.neverland.game.util.runLater
import dev.zorsh.neverland.game.util.runRepeating
import dev.zorsh.neverland.game.util.runSync
import dev.zorsh.neverland.game.util.showToEveryone
import dev.zorsh.neverland.game.network.PacketManager
import dev.zorsh.neverland.game.ui.CanvasSprite
import dev.zorsh.neverland.game.ui.ShaderTextColor
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.SoundStop
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title.Times
import net.kyori.adventure.title.Title.title
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import java.time.Duration
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

class DeltarunePlayer(val uuid: UUID) {

    var player: Player? = null

    init {
        player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid)
    }

    var myBattleUUID: UUID? = null

    var locked = false

    var initialLocation: Pos? = null

    var hp = 1000
    var maxhp = 1000

    var noDamageTicks = 0

    var tpGain = 0
    var tpAmount = 0.0

    private var prevInput = InputHolder()

    var canMoveSoul = false

    private var gameMode = GameMode.SURVIVAL

    fun updatePlayer() {
        player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid)
    }

    fun damage(amount: Int, afterHpCalculated: (Int) -> Unit) {
        if (myBattleUUID == null) return
        player?.playSound(Sound.sound(Key.key("soul_hurt"), Sound.Source.MASTER, 1f, 1f))
        hp = max(hp - amount, 0)

        afterHpCalculated(hp)

        noDamageTicks = 10
        runRepeating(10) { _, _ ->
            noDamageTicks--
        }
    }

    fun tpGain() {
        if (tpGain <= 0) {
            player?.playSound(Sound.sound(Key.key("tp_gain"), Sound.Source.MASTER, 1f, 1f))
        }
        tpGain = 3
    }

    fun freeFromBattle(targetUUID: UUID, silentQuit: Boolean) {
        if (myBattleUUID == null) return
        if (myBattleUUID != targetUUID) return
        player?.sendMessage(Component.text("Freeing from battle $myBattleUUID"))
        if (!silentQuit) {
            myBattleUUID?.let { ids -> BattleManager.getBattle(ids)?.removePlayer(this) }
        }
        myBattleUUID = null
        locked = false
        player?.stopSound(SoundStop.all())
        //
        runLater(1) {
            if (player != null) {
                PacketManager.setAttribute(
                    Attribute.JUMP_STRENGTH,
                    0.42,
                    player!!.entityId,
                    listOf(player!!)
                )
            }
        }
        runSync {
            if (player != null) {
                MinecraftServer.getCommandManager().dispatcher.execute(
                    MinecraftServer.getCommandManager().consoleSender,
                    "sendshaderdata ${player!!.name} 0"
                )
            }
        }
        player?.showTitle(
            title(
                fontText("\uD701", "#000000", "space:default"),
                Component.text(""),
                Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(100))
            )
        )
        runLater(6) {
            player?.gameMode = gameMode
            if (initialLocation != null) {
                player?.teleport(initialLocation!!)
            }
            player?.showToEveryone()
        }
    }

    var battleCenterLoc: Pos? = null

    fun lockInBattle(location: Pos) {
        val uuid = myBattleUUID ?: return
        if (player != null) {
            player?.sendMessage(Component.text("Locking to battle $uuid"))
            val myPlayer = player!!
            initialLocation = myPlayer.position
            myPlayer.teleport(location)
            gameMode = myPlayer.gameMode
            myPlayer.gameMode = GameMode.ADVENTURE
            locked = true
            myPlayer.hideFromEveryone()
            battleCenterLoc = location
            runInfinite(1) { i, action ->
                if (!myPlayer.isOnline) {
                    locked = false
                }

                if (myBattleUUID == null || !BattleManager.hasBattle(myBattleUUID!!)) {
                    locked = false
                }

                if (!locked) {
                    action.cancel()
                    freeFromBattle(uuid, false)
                } else {
                    if (i % 20 == 0) {
                        myPlayer.hideFromEveryone()
                        PacketManager.setAttribute(
                            Attribute.JUMP_STRENGTH,
                            0.0,
                            myPlayer.entityId,
                            listOf(myPlayer)
                        )
                    }
                    if (tpGain > 0) {
                        tpGain--
                        tpAmount = min(tpAmount + 0.5, 100.0)
                    }

                    myPlayer.lookAt(myPlayer.position.add(0.0, -1000000000.0, 100.0))

                    val target = location.withY(myPlayer.position.y)
                    val playerOffset = myPlayer.position.sub(target)

                    if (!canMoveSoul) {
                        if (target.distance(myPlayer.position) > 0.02) {
                            val v = Vec(playerOffset.x * -0.15, playerOffset.y * -0.15, playerOffset.z * -0.15)
                            myPlayer.velocity = v
                        }
                    }
                }
            }
        }
    }

    val savedSouls = mutableMapOf<String, UUID>()
    fun unlockSoul(battleBoxLocation: Pair<Double, Double>, canvas: BattleCanvas, players: List<Player>) {
        canMoveSoul = true
        players.forEach { pl ->
            val uuid = UUID.randomUUID()
            canvas.myCanvas.drawSprite(
                0.0, 20.0, 1.0, 1.0, 5,
                CanvasSprite.SOUL_OTHER, ShaderTextColor.pure("#880000"), "soul_for_others_$uuid", pl
            )
            savedSouls["soul_for_others_$uuid"] = pl.uuid
        }
        val list = savedSouls.toMap()
        runInfinite(1) { _, action ->
            if (!locked || !canMoveSoul || battleCenterLoc == null || player == null) {
                savedSouls.forEach { (name, uuid) ->
                    canvas.myCanvas.remove(name, uuid)
                }
                savedSouls.clear()
                action.cancel()
            } else {
                val pos = player!!.position.sub(battleCenterLoc!!)
                val x = -pos.x * 16 * 8
                val y = pos.z * 16 * 8
                list.forEach { (name, uuid) ->
                    canvas.myCanvas.setPosition(x + battleBoxLocation.first, y + battleBoxLocation.second, name, uuid)
                }
            }
        }
    }

    fun lockSoul() {
        canMoveSoul = false
    }

    var playerActionSelector: PlayerActionSelector? = null
    fun handlePickingOption(battleCanvas: BattleCanvas) {
        playerActionSelector = PlayerActionSelector(battleCanvas, this)
        playerActionSelector?.startUpdate()
    }

    fun stopPickingOption() {
        playerActionSelector?.endUpdate()
        playerActionSelector = null
    }

    fun onLeftPressed() {
        playerActionSelector?.onLeftPressed()
    }

    fun onRightPressed() {
        playerActionSelector?.onRightPressed()
    }

    fun onForwardPressed() {
        playerActionSelector?.onForwardPressed()
    }

    fun onBackwardPressed() {
        playerActionSelector?.onBackwardPressed()
    }

    fun onJumpPressed() {
        playerActionSelector?.onJumpPressed()
    }

    fun onSneakPressed() {
        playerActionSelector?.onSneakPressed()
    }

    fun onSprintPressed() {
        playerActionSelector?.onSprintPressed()
    }

    fun updateInputs(newInput: InputHolder) {
        if (newInput.left && !prevInput.left) {
            onLeftPressed()
        }
        if (newInput.right && !prevInput.right) {
            onRightPressed()
        }
        if (newInput.forward && !prevInput.forward) {
            onForwardPressed()
        }
        if (newInput.backward && !prevInput.backward) {
            onBackwardPressed()
        }
        if (newInput.jump && !prevInput.jump) {
            onJumpPressed()
        }
        if (newInput.sneak && !prevInput.sneak) {
            onSneakPressed()
        }
        if (newInput.sprint && !prevInput.sprint) {
            onSprintPressed()
        }
        prevInput = newInput
    }
}