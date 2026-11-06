package dev.zorsh.neverland.game.ui

import dev.zorsh.neverland.game.network.FakeTextDisplay
import dev.zorsh.neverland.game.network.PacketManager
import dev.zorsh.neverland.game.util.FakeDisplayData
import dev.zorsh.neverland.game.util.Quaternion
import dev.zorsh.neverland.game.util.Transformation
import dev.zorsh.neverland.game.util.color
import dev.zorsh.neverland.game.util.font
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class PlayerUICanvas {
    var targetPlayers = CopyOnWriteArrayList<Player>()

    data class SpriteScalingHolder(
        val entity: FakeTextDisplay,
        val scaling: Pair<Double, Double>
    )

    var objects = CopyOnWriteArrayList<FakeTextDisplay>()

    // savedObjects[name] = (entity: display, scaling: (x, y))
    var savedObjects = ConcurrentHashMap<String, SpriteScalingHolder>()
    var objectsPerPlayer = ConcurrentHashMap<UUID, CopyOnWriteArrayList<FakeTextDisplay>>()

    // savedObjects[player.uniqueId][name] = (entity: display, scaling: (x, y))
    var savedObjectsPerPlayer = ConcurrentHashMap<UUID, ConcurrentHashMap<String, SpriteScalingHolder>>()

    val TRANSLATION_BIAS = -0.2
    val MOUNT_LOCATION_OFFSET = Vec(0.0, -TRANSLATION_BIAS, 0.0)

    fun initialize(players: List<Player>) {
        targetPlayers = CopyOnWriteArrayList(players)
    }

    fun clear() {
        // Global objects
        objects.forEach {
            it.destroy()
        }
        objects.clear()
        savedObjects.clear()

        // Individual objects
        objectsPerPlayer.forEach { (_, list) ->
            list.forEach { it.destroy() }
        }
        objectsPerPlayer.clear()
        savedObjectsPerPlayer.clear()
    }

    fun removePlayer(player: Player) {
        targetPlayers -= player
        clearPlayer(player)
    }

    fun clearPlayer(player: Player) {
        objectsPerPlayer[player.uuid]?.forEach {
            it.destroy()
        }
        objectsPerPlayer[player.uuid]?.clear()
        savedObjectsPerPlayer[player.uuid]?.clear()
        objects.forEach {
            it.destroy(player)
        }
    }

    fun updateCanvas(player: Player) {
        val playerEntities = objectsPerPlayer[player.uuid]?.map { it.entityId } ?: emptyList()
        val allEntities = objects.map { it.entityId }.toMutableList()
        allEntities.addAll(playerEntities)
        PacketManager.mountEntities(player.entityId, allEntities, listOf(player))
        allEntities.clear()
    }

    fun updateCanvas() {
        if (targetPlayers.isEmpty()) return
        targetPlayers.forEach { pl ->
            updateCanvas(pl)
        }
    }

    fun remove(objName: String, playerUUID: UUID? = null) {
        if (playerUUID == null) {
            val pair = savedObjects[objName] ?: return
            val obj = pair.entity
            objects.remove(obj)
            savedObjects.remove(objName)
            obj.destroy()
        } else {
            val pair = savedObjectsPerPlayer[playerUUID]?.get(objName) ?: return
            val obj = pair.entity
            objectsPerPlayer[playerUUID]?.remove(obj)
            savedObjectsPerPlayer[playerUUID]?.remove(objName)
            obj.destroy()
        }
    }

    fun setZ(z: Int, objName: String, playerUUID: UUID? = null) {
        fun getObj() =
            if (playerUUID == null) savedObjects[objName]?.entity else savedObjectsPerPlayer[playerUUID]?.get(objName)?.entity

        val obj = getObj() ?: return
        obj.changeOnlyTransformation(
            Transformation(
                Vec(obj.transformation.translation.x, obj.transformation.translation.y, z / 255.0),
                obj.transformation.leftRotation,
                obj.transformation.scale,
                obj.transformation.rightRotation
            )
        )
    }

    fun setRotation(angle: Float, objName: String, playerUUID: UUID? = null) {
        val holder =
            (if (playerUUID == null) savedObjects[objName] else savedObjectsPerPlayer[playerUUID]?.get(objName))
                ?: return

        val obj = holder.entity
        obj.changeOnlyTransformation(
            Transformation(
                obj.transformation.translation,
                Quaternion(angle, 0f, 0f, 1f),
                obj.transformation.scale,
                obj.transformation.rightRotation
            )
        )
    }

    fun rotate(angle: Float, objName: String, playerUUID: UUID? = null) {
        val holder =
            (if (playerUUID == null) savedObjects[objName] else savedObjectsPerPlayer[playerUUID]?.get(objName))
                ?: return

        val obj = holder.entity
        val currentAngle = obj.transformation.leftRotation.angle()
        obj.changeOnlyTransformation(
            Transformation(
                obj.transformation.translation,
                Quaternion.fromAxisAngle(currentAngle + angle, 0f, 0f, 1f),
                obj.transformation.scale,
                obj.transformation.rightRotation
            )
        )
    }

    fun setSprite(sprite: CanvasSprite, color: ShaderTextColor, objName: String, playerUUID: UUID? = null) {
        val holder =
            (if (playerUUID == null) savedObjects[objName] else savedObjectsPerPlayer[playerUUID]?.get(objName))
                ?: return

        val obj = holder.entity
        val size = getScale(objName, playerUUID)
        obj.changeText(sprite.toTextValue().color(color.value))
        if (playerUUID == null) {
            savedObjects[objName] = SpriteScalingHolder(obj, sprite.getSizeRatios())
        } else {
            savedObjectsPerPlayer[playerUUID]?.set(objName, SpriteScalingHolder(obj, sprite.getSizeRatios()))
        }
        setScale(size.first, size.second, objName, playerUUID)
    }

    fun setText(text: Component, objName: String, playerUUID: UUID? = null) {
        val holder =
            (if (playerUUID == null) savedObjects[objName] else savedObjectsPerPlayer[playerUUID]?.get(objName))
                ?: return

        val obj = holder.entity
        val size = getScale(objName, playerUUID)
        obj.changeText(text)
        if (playerUUID == null) {
            savedObjects[objName] = SpriteScalingHolder(obj, 1.0 to 1.0)
        } else {
            savedObjectsPerPlayer[playerUUID]?.set(objName, SpriteScalingHolder(obj, 1.0 to 1.0))
        }
        setScale(size.first, size.second, objName, playerUUID)
    }

    fun getText(objName: String, playerUUID: UUID? = null): Component {
        val obj =
            (if (playerUUID == null) savedObjects[objName]?.entity else savedObjectsPerPlayer[playerUUID]?.get(objName)?.entity)
                ?: throw IllegalStateException("Object $objName not found")

        return obj.text
    }

    fun hasObject(objName: String, playerUUID: UUID? = null) =
        if (playerUUID == null) savedObjects.contains(objName) else (savedObjectsPerPlayer[playerUUID]?.contains(objName) == true)

    fun setScale(sx: Double, sy: Double, objName: String, playerUUID: UUID? = null) {
        val holder =
            (if (playerUUID == null) savedObjects[objName] else savedObjectsPerPlayer[playerUUID]?.get(objName))
                ?: return

        val obj = holder.entity
        val scaling = holder.scaling
        obj.changeOnlyTransformation(
            Transformation(
                obj.transformation.translation,
                obj.transformation.leftRotation,
                Vec(sx * scaling.first * 2.5, sy * scaling.second * 2.5, obj.transformation.scale.z),
                obj.transformation.rightRotation
            )
        )
    }

    fun getScale(objName: String, playerUUID: UUID? = null): Pair<Double, Double> {
        val holder =
            (if (playerUUID == null) savedObjects[objName] else savedObjectsPerPlayer[playerUUID]?.get(objName))
                ?: throw IllegalStateException("Object $objName not found")

        val obj = holder.entity
        val scaling = holder.scaling

        return obj.transformation.scale.x / scaling.first / 2.5 to obj.transformation.scale.y / scaling.second / 2.5
    }

    fun setPosition(px: Double, py: Double, objName: String, playerUUID: UUID? = null) {
        fun getObj() =
            if (playerUUID == null) savedObjects[objName]?.entity else savedObjectsPerPlayer[playerUUID]?.get(objName)?.entity

        val obj = getObj() ?: return
        obj.changeOnlyTransformation(
            Transformation(
                Vec(px / 16, py / 16 + TRANSLATION_BIAS, obj.transformation.translation.z),
                obj.transformation.leftRotation,
                obj.transformation.scale,
                obj.transformation.rightRotation
            )
        )
    }

    fun getPosition(objName: String, playerUUID: UUID? = null): Pair<Double, Double> {
        val holder =
            (if (playerUUID == null) savedObjects[objName] else savedObjectsPerPlayer[playerUUID]?.get(objName))
                ?: throw IllegalStateException("Object $objName not found")

        val obj = holder.entity

        return obj.transformation.translation.x * 16f to (obj.transformation.translation.y - TRANSLATION_BIAS) * 16f
    }

    fun move(ox: Double, oy: Double, objName: String, playerUUID: UUID? = null) {
        fun getObj() =
            if (playerUUID == null) savedObjects[objName]?.entity else savedObjectsPerPlayer[playerUUID]?.get(objName)?.entity

        val obj = getObj() ?: return
        obj.changeOnlyTransformation(
            Transformation(
                obj.transformation.translation.add(Vec(ox / 16, oy / 16, 0.0)),
                obj.transformation.leftRotation,
                obj.transformation.scale,
                obj.transformation.rightRotation
            )
        )
    }

    private fun handleDraw(
        drawFunction: (List<Player>, (FakeTextDisplay) -> Unit) -> Unit,
        scaling: Pair<Double, Double> = 1.0 to 1.0,
        saveAs: String? = null,
        player: Player? = null,
        afterSpawn: () -> Unit
    ) {
        if (player == null) {
            if (targetPlayers.isEmpty()) return
            drawFunction(targetPlayers) { ent ->
                objects += ent
                if (saveAs != null) {
                    savedObjects[saveAs] = SpriteScalingHolder(ent, scaling)
                }
                updateCanvas()
                afterSpawn()
            }
        } else {
            drawFunction(listOf(player)) { ent ->
                objectsPerPlayer.getOrPut(player.uuid, defaultValue = { return@getOrPut CopyOnWriteArrayList() }) += ent
                if (saveAs != null) {
                    savedObjectsPerPlayer.getOrPut(
                        player.uuid,
                        defaultValue = { return@getOrPut ConcurrentHashMap() })[saveAs] = SpriteScalingHolder(ent, scaling)
                }
                updateCanvas(player)
                afterSpawn()
            }
        }
    }

    fun drawRect(
        sx: Double,
        sy: Double,
        dx: Double,
        dy: Double,
        z: Int,
        color: ShaderTextColor,
        saveAs: String? = null,
        player: Player? = null,
        afterSpawn: () -> Unit = {},
    ) {
        fun drawToPlayers(players: List<Player>, after: (FakeTextDisplay) -> Unit) {
            if (players.isEmpty()) return
            val actualPlayer = players[0]
            val loc = actualPlayer.position.add(0.0, actualPlayer.eyeHeight, 0.0).add(MOUNT_LOCATION_OFFSET).withView(0f, 0f)
            val scaleX = dx - sx + 1
            val scaleY = dy - sy + 1
            PacketManager.spawnTextDisplay(
                loc,
                Component.text("\uF001").font("space:dbattle").color(color.value),
                players,
                FakeDisplayData(
                    Transformation(
                        Vec(sx / 16, sy / 16 + TRANSLATION_BIAS, z / 255.0),
                        Quaternion(0f, 0f, 0f, 1f),
                        Vec(2.5 * scaleX, 2.5 * scaleY, 1.0),
                        Quaternion(0f, 0f, 0f, 1f)
                    ),
                    opacity = 253.toByte()
                ),
                false,
                TextDisplayMeta.Alignment.CENTER,
                1000,
                false
            ) { ent ->
                after(ent)
            }
        }

        handleDraw(::drawToPlayers, 1.0 to 1.0, saveAs, player, afterSpawn)
    }

    fun drawMouse(
        saveAs: String? = null,
        player: Player? = null,
        afterSpawn: () -> Unit = {},
    ) {
        val scaling = CanvasSprite.MOUSE.getSizeRatios()
        fun drawToPlayers(players: List<Player>, after: (FakeTextDisplay) -> Unit) {
            if (players.isEmpty()) return
            val actualPlayer = players[0]
            val loc = actualPlayer.position.add(0.0, actualPlayer.eyeHeight, 0.0).add(MOUNT_LOCATION_OFFSET).withView(0f, 0f)
            PacketManager.spawnTextDisplay(
                loc,
                CanvasSprite.MOUSE.toTextValue().color("#ffffff"),
                players,
                FakeDisplayData(
                    Transformation(
                        Vec(0.0, TRANSLATION_BIAS, 1 / 255.0),
                        Quaternion(0f, 0f, 0f, 1f),
                        Vec(5f * scaling.first, 5f * scaling.second, 1.0),
                        Quaternion(0f, 0f, 0f, 1f)
                    ),
                    opacity = 252.toByte()
                ),
                false,
                TextDisplayMeta.Alignment.CENTER,
                1000,
                false
            ) { ent ->
                after(ent)
            }
        }

        handleDraw(::drawToPlayers, scaling, saveAs, player, afterSpawn)
    }

    fun drawSprite(
        px: Double,
        py: Double,
        sx: Double,
        sy: Double,
        z: Int,
        sprite: CanvasSprite,
        color: ShaderTextColor,
        saveAs: String? = null,
        player: Player? = null,
        afterSpawn: () -> Unit = {},
    ) {
        val scaling = sprite.getSizeRatios()
        fun drawToPlayers(players: List<Player>, after: (FakeTextDisplay) -> Unit) {
            if (players.isEmpty()) return
            val actualPlayer = players[0]
            val loc = actualPlayer.position.add(0.0, actualPlayer.eyeHeight, 0.0).add(MOUNT_LOCATION_OFFSET).withView(0f, 0f)
            PacketManager.spawnTextDisplay(
                loc,
                sprite.toTextValue().color(color.value),
                players,
                FakeDisplayData(
                    Transformation(
                        Vec(px / 16, py / 16f + TRANSLATION_BIAS, z / 255.0),
                        Quaternion(0f, 0f, 0f, 1f),
                        Vec(2.5f * scaling.first * sx, 2.5f * scaling.second * sy, 1.0),
                        Quaternion(0f, 0f, 0f, 1f)
                    ),
                    opacity = 253.toByte()
                ),
                false,
                TextDisplayMeta.Alignment.CENTER,
                1000,
                false
            ) { ent ->
                after(ent)
            }
        }

        handleDraw(::drawToPlayers, scaling, saveAs, player, afterSpawn)
    }

    fun drawText(
        px: Double,
        py: Double,
        sx: Double,
        sy: Double,
        z: Int,
        text: Component,
        color: ShaderTextColor?,
        alignment: TextDisplayMeta.Alignment = TextDisplayMeta.Alignment.CENTER,
        lineWidth: Int = 1000,
        saveAs: String? = null,
        player: Player? = null,
        afterSpawn: () -> Unit = {},
    ) {
        fun drawToPlayers(players: List<Player>, after: (FakeTextDisplay) -> Unit) {
            if (players.isEmpty()) return
            val actualPlayer = players[0]
            val loc = actualPlayer.position.add(0.0, actualPlayer.eyeHeight, 0.0).add(MOUNT_LOCATION_OFFSET).withView(0f, 0f)
            PacketManager.spawnTextDisplay(
                loc,
                if (color != null) text.color(color.value) else text,
                players,
                FakeDisplayData(
                    Transformation(
                        Vec(px / 16, py / 16 + TRANSLATION_BIAS, z / 255.0),
                        Quaternion(0f, 0f, 0f, 1f),
                        Vec(2.5 * sx, 2.5 * sy, 1.0),
                        Quaternion(0f, 0f, 0f, 1f)
                    ),
                    opacity = 253.toByte()
                ),
                false,
                alignment,
                lineWidth,
                false
            ) { ent ->
                after(ent)
            }
        }

        handleDraw(::drawToPlayers, 1.0 to 1.0, saveAs, player, afterSpawn)
    }
}