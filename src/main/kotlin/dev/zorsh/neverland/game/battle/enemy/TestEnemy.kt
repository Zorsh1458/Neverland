package dev.zorsh.neverland.game.battle.enemy

import dev.zorsh.neverland.game.Neverland
import dev.zorsh.neverland.game.battle.projectile.CircleHitbox
import dev.zorsh.neverland.game.battle.projectile.ProjectileData
import dev.zorsh.neverland.game.ui.CanvasSprite
import dev.zorsh.neverland.game.ui.ShaderTextColor
import dev.zorsh.neverland.game.util.runLater
import dev.zorsh.neverland.game.util.runRepeating
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import java.lang.Math.clamp
import kotlin.math.cos
import kotlin.math.sin

class TestEnemy(
    name: String,
    maxHitpoints: Int,
    canvasSprites: List<CanvasSprite>
) : SpritedEnemy(
    name,
    maxHitpoints,
    listOf(
        Component.text("Это что еще за балбес"),
        Component.text("Тестовый враг встал у вас на пути!"),
        Component.text("Полный скебоб..")
    ),
    canvasSprites,
    6
) {

    override suspend fun attack(onAttackEnds: () -> Unit) = coroutineScope {
        val count = 10
        var yOffset = 0f
        repeat(count) { _ ->
            yOffset += Neverland.random.nextFloat() * 16 - 8
            yOffset = clamp(yOffset, -64f, 64f)
            attackPattern2(yOffset)
            delay(600)
        }
        delay(500)
    }

    fun attackPattern1(initialAngle: Double) {
        val (bbx, bby) = myBattle.getBBLocation()
        var radius = 100
        var angle = initialAngle
        val inx = cos(angle) * radius + bbx
        val iny = sin(angle) * radius + bby
        myBattle.createProjectile(
            inx, iny,
            ProjectileData(
                listOf(CanvasSprite.SOUL),
                null,
                CircleHitbox(8f)
            )
        ) { canvas, registryName, dealDamage ->
            runRepeating(120) { i ->
                if (i < 20) {
                    radius -= 2
                }
                val rad = radius + sin(i * 0.15) * 10
                angle -= 3.1415 * 0.03
                val x = cos(angle) * rad + bbx
                val y = sin(angle) * rad + bby
                canvas.setPosition(x, y, registryName)
                if (i < 118) {
                    dealDamage(Neverland.random.nextInt(10) + 10)
                }

                if (i == 117) {
                    canvas.setScale(0.5, 0.5, registryName)
                }
                if (i == 118) {
                    canvas.setScale(0.0, 0.0, registryName)
                }
            }
            runLater(121) {
                canvas.remove(registryName)
            }
        }
    }

    fun attackPattern2(yOffset: Float) {
        attackPattern2_projectile(yOffset + 48f)
        attackPattern2_projectile(yOffset + 80f)
        attackPattern2_projectile(yOffset - 48f)
        attackPattern2_projectile(yOffset - 80f)
    }

    fun attackPattern2_projectile(yOffset: Float) {
        val (bbx, bby) = myBattle.getBBLocation()
        myBattle.createProjectile(
            bbx + 256, bby + yOffset,
            ProjectileData(
                listOf(CanvasSprite.SQUARE),
                null,
                CircleHitbox(8f)
            )
        ) { canvas, registryName, dealDamage ->
            val count = 30
            runLater(2) {
                canvas.setScale(4.0, 16.0, registryName)
            }
            runRepeating(count) { i ->
                canvas.move(-20.0, 0.0, registryName)
                if (i < count-2) {
                    dealDamage(Neverland.random.nextInt(10) + 10)
                }
            }
            runLater(-3 + count) {
                canvas.setScale(0.5, 0.5, registryName)
            }
            runLater(-2 + count) {
                canvas.setScale(0.0, 0.0, registryName)
            }
            runLater(1 + count) {
                canvas.remove(registryName)
            }
        }
        myBattle.createProjectile(
            bbx + 256, bby + yOffset,
            ProjectileData(
                listOf(CanvasSprite.SQUARE),
                null,
                CircleHitbox(8f)
            )
        ) { canvas, registryName, _ ->
            val count = 30
            runLater(1) {
                canvas.setZ(40, registryName)
                canvas.setSprite(CanvasSprite.SQUARE, ShaderTextColor.pure("#000000"), registryName)
            }
            runLater(2) {
                canvas.setScale(3.0, 15.0, registryName)
            }
            runRepeating(count) { _ ->
                canvas.move(-20.0, 0.0, registryName)
            }
            runLater(-3 + count) {
                canvas.setScale(0.5, 0.5, registryName)
            }
            runLater(-2 + count) {
                canvas.setScale(0.0, 0.0, registryName)
            }
            runLater(1 + count) {
                canvas.remove(registryName)
            }
        }
    }
}