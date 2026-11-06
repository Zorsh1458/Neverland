package dev.zorsh.neverland.game.network

import dev.zorsh.neverland.game.util.FakeDisplayData
import dev.zorsh.neverland.game.util.Quaternion
import dev.zorsh.neverland.game.util.Transformation
import dev.zorsh.neverland.game.util.runLater
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minestom.server.MinecraftServer
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Metadata
import net.minestom.server.entity.MetadataDef
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.SendablePacket
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket
import net.minestom.server.network.packet.server.play.EntityAttributesPacket
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket
import net.minestom.server.network.packet.server.play.PlayerPositionAndLookPacket
import net.minestom.server.network.packet.server.play.SetPassengersPacket
import net.minestom.server.network.packet.server.play.SpawnEntityPacket
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Suppress("UNUSED", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class PacketManager {
    companion object {
        @Volatile
        private var counter = AtomicInteger(10000)

        fun newEntityId() = counter.incrementAndGet()

        @JvmStatic
        fun setTransformation(
            entityId: Int,
            newTransformation: Transformation,
            players: List<Player>,
            interpolationDuration: Int = 1,
            teleportDuration: Int = 2,
        ) {
            val packet = getDisplayMetadataPacket(
                entityId,
                newTransformation,
                interpolationDuration,
                teleportDuration
            )

            for (player in players.filter { it.isOnline }) {
                player.sendPacket(packet)
            }
        }

        @JvmStatic
        fun setTextDisplayMetadata(
            entityId: Int,
            newText: Component,
            newTransformation: Transformation,
            players: List<Player>,
            interpolationDuration: Int = 1,
            teleportDuration: Int = 2,
            opacity: Byte,
        ) {
            val packet = getTextDisplayMetadataPacket(
                entityId,
                newText,
                newTransformation,
                interpolationDuration,
                teleportDuration,
                opacity
            )

            for (player in players.filter { it.isOnline }) {
                player.sendPacket(packet)
            }
        }

        @JvmStatic
        fun getTextDisplayMetadataPacket(
            entityId: Int,
            newText: Component,
            newTransformation: Transformation,
            interpolationDuration: Int,
            teleportDuration: Int,
            opacity: Byte,
        ) = EntityMetaDataPacket(
            entityId,
            mapOf(
                8 to Metadata.VarInt(0),
                9 to Metadata.VarInt(interpolationDuration),
                10 to Metadata.VarInt(teleportDuration),
                11 to Metadata.Vector3(newTransformation.translation),
                12 to Metadata.Vector3(newTransformation.scale),
                13 to newTransformation.leftRotation.metadataEntry,
                14 to newTransformation.rightRotation.metadataEntry,
                23 to Metadata.Chat(newText),
                26 to Metadata.Byte(opacity)
            )
        )

        @JvmStatic
        fun getDisplayMetadataPacket(
            entityId: Int,
            newTransformation: Transformation,
            interpolationDuration: Int,
            teleportDuration: Int,
        ) = EntityMetaDataPacket(
            entityId,
            mapOf(
                8 to Metadata.VarInt(0),
                9 to Metadata.VarInt(interpolationDuration),
                10 to Metadata.VarInt(teleportDuration),
                11 to Metadata.Vector3(newTransformation.translation),
                12 to Metadata.Vector3(newTransformation.scale),
                13 to newTransformation.leftRotation.metadataEntry,
                14 to newTransformation.rightRotation.metadataEntry
            )
        )

        @JvmStatic
        fun setAttribute(
            attribute: Attribute,
            base: Double,
            entityId: Int,
            players: List<Player>,
        ) {
            val packet = EntityAttributesPacket(
                entityId,
                listOf(
                    EntityAttributesPacket.Property(
                        attribute,
                        base,
                        emptyList()
                    )
                )
            )

            for (player in players.filter { it.isOnline }) {
                player.sendPacket(packet)
            }
        }

        @JvmStatic
        fun mountEntities(
            mountId: Int,
            entities: List<Int>,
            players: List<Player>,
        ) {
            val packet = SetPassengersPacket(mountId, entities)

            for (player in players.filter { it.isOnline }) {
                player.sendPacket(packet)
            }
        }

        @JvmStatic
        fun spawnHitbox(
            location: Pos,
            scale: Double,
            players: List<Player>,
            afterSpawned: (Int, Int) -> Unit,
        ) {
            val shulkerId = newEntityId()

            val packetScale = EntityAttributesPacket(
                shulkerId,
                listOf(
                    EntityAttributesPacket.Property(
                        Attribute.SCALE,
                        scale,
                        emptyList()
                    )
                )
            )

            val packetShulker = SpawnEntityPacket(
                shulkerId,
                UUID.randomUUID(),
                EntityType.SHULKER.id(),
                location,
                0f,
                0,
                0,
                0,
                0
            )

            val anchorId = newEntityId()

            val packetAnchor = SpawnEntityPacket(
                anchorId,
                UUID.randomUUID(),
                EntityType.BLOCK_DISPLAY.id(),
                location,
                0f,
                0,
                0,
                0,
                0
            )

            val packetMount = SetPassengersPacket(anchorId, listOf(shulkerId))

            for (player in players.filter { it.isOnline }) {
                player.sendPacket(packetShulker)
                player.sendPacket(packetScale)
                player.sendPacket(packetAnchor)
                player.sendPacket(packetMount)
            }

            afterSpawned(anchorId, shulkerId)
        }

        @JvmStatic
        fun spawnShaderEffector(
            location: Pos,
            players: List<Player>,
            afterSpawned: (FakeItemDisplay) -> Unit,
        ) {
            val entityId = newEntityId()
            val spawnPacket = SpawnEntityPacket(
                entityId,
                UUID.randomUUID(),
                EntityType.ITEM_DISPLAY.id(),
                location,
                0f,
                0,
                0,
                0,
                0
            )
            val metadataPacket = EntityMetaDataPacket(
                entityId,
                mapOf(
                    0 to Metadata.Byte(64),
                    8 to Metadata.VarInt(0),
                    9 to Metadata.VarInt(0),
                    10 to Metadata.VarInt(0),
                    12 to Metadata.Vector3(Vec(50.0, 50.0, 0.01)),
                    22 to Metadata.VarInt(Color(253, 0, 6).asRGB()),
                    23 to Metadata.ItemStack(ItemStack.of(Material.STONE))
                )
            )

            for (player in players.filter { it.isOnline }) {
                player.sendPacket(spawnPacket)
                player.sendPacket(metadataPacket)
            }

            afterSpawned(
                FakeItemDisplay(
                    entityId,
                    Transformation(
                        Vec(0.0),
                        Quaternion(0f, 0f, 0f, 1f),
                        Vec(50.0, 50.0, 0.01),
                        Quaternion(0f, 0f, 0f, 1f)
                    ),
                    0, 0, players.filter { it.isOnline }
                )
            )
        }

        @JvmStatic
        fun spawnItemDisplay(
            location: Pos,
            item: ItemStack,
            players: List<Player>,
            data: FakeDisplayData,
            afterSpawned: (FakeItemDisplay) -> Unit,
        ) {
            val entityId = newEntityId()
            val spawnPacket = SpawnEntityPacket(
                entityId,
                UUID.randomUUID(),
                EntityType.ITEM_DISPLAY.id(),
                location,
                0f,
                0,
                0,
                0,
                0
            )
            val metadataPacket = EntityMetaDataPacket(
                entityId,
                mapOf(
                    8 to Metadata.VarInt(0),
                    9 to Metadata.VarInt(data.interpolationDuration),
                    10 to Metadata.VarInt(data.teleportDuration),
                    11 to Metadata.Vector3(data.transformation.translation),
                    12 to Metadata.Vector3(data.transformation.scale),
                    13 to data.transformation.leftRotation.metadataEntry,
                    14 to data.transformation.rightRotation.metadataEntry,
                    23 to Metadata.ItemStack(item)
                )
            )

            for (player in players.filter { it.isOnline }) {
                player.sendPacket(spawnPacket)
                player.sendPacket(metadataPacket)
            }

            afterSpawned(
                FakeItemDisplay(
                    entityId,
                    data.transformation,
                    data.teleportDuration,
                    data.interpolationDuration,
                    players.filter { it.isOnline }
                )
            )
        }

        @JvmStatic
        fun spawnTextDisplay(
            location: Pos,
            text: Component,
            players: List<Player>,
            data: FakeDisplayData,
            seeThrough: Boolean,
            alignment: TextDisplayMeta.Alignment,
            lineWidth: Int,
            isShadowed: Boolean,
            afterSpawned: (FakeTextDisplay) -> Unit,
        ) {
            val entityId = newEntityId()
            val spawnPacket = SpawnEntityPacket(
                entityId,
                UUID.randomUUID(),
                EntityType.TEXT_DISPLAY.id(),
                location,
                0f,
                0,
                0,
                0,
                0
            )

            var bitmask = 0
            if (isShadowed) {
                bitmask += 1
            }
            if (seeThrough) {
                bitmask += 2
            }
            if (alignment == TextDisplayMeta.Alignment.LEFT) {
                bitmask += 8
            } else if (alignment == TextDisplayMeta.Alignment.RIGHT) {
                bitmask += 16
            }
            val metadataPacket = EntityMetaDataPacket(
                entityId,
                mapOf(
                    8 to Metadata.VarInt(0),
                    9 to Metadata.VarInt(data.interpolationDuration),
                    10 to Metadata.VarInt(data.teleportDuration),
                    11 to Metadata.Vector3(data.transformation.translation),
                    12 to Metadata.Vector3(data.transformation.scale),
                    13 to data.transformation.leftRotation.metadataEntry,
                    14 to data.transformation.rightRotation.metadataEntry,
                    16 to Metadata.VarInt(15 * 16 + 15 * 1048576),
                    23 to Metadata.Chat(text),
                    24 to Metadata.VarInt(lineWidth),
                    25 to Metadata.VarInt(0),
                    26 to Metadata.Byte(data.opacity),
                    27 to Metadata.Byte(bitmask.toByte())
                )
            )

            for (player in players.filter { it.isOnline }) {
                player.sendPacket(spawnPacket)
                player.sendPacket(metadataPacket)
            }

            afterSpawned(
                FakeTextDisplay(
                    entityId,
                    text,
                    data.transformation,
                    data.teleportDuration,
                    data.interpolationDuration,
                    players.filter { it.isOnline },
                    opacity = data.opacity
                )
            )
        }

        @JvmStatic
        fun spawnPlayer(player: Player, players: List<Player>) {
            val spawnPacket = SpawnEntityPacket(
                player.entityId,
                UUID.randomUUID(),
                EntityType.PLAYER.id(),
                player.position,
                0f,
                0,
                0,
                0,
                0
            )

            for (player in players.filter { it.isOnline }) {
                player.sendPacket(spawnPacket)
                player.sendPacket(player.metadataPacket)
            }
        }

        @JvmStatic
        fun removeEntity(entityId: Int, players: List<Player>) {
            val packet = DestroyEntitiesPacket(entityId)

            for (player in players.filter { it.isOnline }) {
                player.sendPacket(packet)
            }
        }
    }
}