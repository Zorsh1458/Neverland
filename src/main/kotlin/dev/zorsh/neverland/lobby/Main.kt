package dev.zorsh.neverland.lobby

import dev.zorsh.neverland.lobby.command.TeleportCommand
import dev.zorsh.neverland.lobby.util.TPSBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.server.ServerTickMonitorEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.WorldBorderCenterPacket
import net.minestom.server.network.packet.server.play.WorldBorderSizePacket
import net.minestom.server.scoreboard.BelowNameTag
import net.minestom.server.world.DimensionType
import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


fun main() {
    val server = MinecraftServer.init()

    val instanceManager = MinecraftServer.getInstanceManager()

    val lobbyDimension = MinecraftServer.getDimensionTypeRegistry().register(
        "neverland:lobby",
        DimensionType.builder()
            .minY(0)
            .height(384)
            .logicalHeight(384)
            .ambientLight(1F)
            .build()
    )
    val instanceContainer = instanceManager.createInstanceContainer(lobbyDimension)
    instanceContainer.setGenerator { unit ->
        unit.modifier().fillHeight(0, 39, Block.DIRT)
        unit.modifier().fillHeight(39, 40, Block.GRASS_BLOCK)
    }

    val globalEventHandler = MinecraftServer.getGlobalEventHandler()

    val random = Random(1458)
    globalEventHandler.addListener(
        AsyncPlayerConfigurationEvent::class.java
    ) { event: AsyncPlayerConfigurationEvent ->
        val player = event.player
        event.spawningInstance = instanceContainer
        player.respawnPoint = Pos(random.nextDouble() * 20.0 - 10.0, 42.0, random.nextDouble() * 20.0 - 10.0)
    }

    val skins = listOf(
        PlayerSkin.fromUsername("Zorsh"),
        PlayerSkin.fromUsername("Fifuz"),
        PlayerSkin.fromUsername("Nialithi"),
        PlayerSkin.fromUsername("Gloomy210"),
        PlayerSkin.fromUsername("NotchDaVinci"),
        PlayerSkin.fromUsername("EssciZ"),
        PlayerSkin.fromUsername("denoph1ne"),
        PlayerSkin.fromUsername("X_Arts_X")
    )
    var currentTick = 0
    globalEventHandler.addListener(
        PlayerSpawnEvent::class.java
    ) { event: PlayerSpawnEvent ->
        val player = event.player
        player.gameMode = GameMode.CREATIVE
        player.skin = skins.random()
        player.setBelowNameTag(BelowNameTag("Test1", Component.text("Test2")))
        player.playerConnection.sendPacket(
            WorldBorderCenterPacket(0.0, 0.0)
        )
        player.playerConnection.sendPacket(
            WorldBorderSizePacket(64.0)
        )
        println("${(player.name as TextComponent).content()} присоединился к лобби")
        instanceContainer.players.forEach { pl ->
            pl.sendMessage(player.name.append(Component.text(" присоединился к лобби")).color(NamedTextColor.YELLOW))
        }
    }
    globalEventHandler.addListener(
        PlayerDisconnectEvent::class.java
    ) { event: PlayerDisconnectEvent ->
        val player = event.player
        println("${(player.name as TextComponent).content()} вышел из лобби")
        instanceContainer.players.forEach { pl ->
            pl.sendMessage(player.name.append(Component.text(" вышел из лобби")).color(NamedTextColor.YELLOW))
        }
    }
    globalEventHandler.addListener(
        PlayerBlockInteractEvent::class.java
    ) { event: PlayerBlockInteractEvent ->
        event.isCancelled = true
    }

    globalEventHandler.addListener(
        ServerTickMonitorEvent::class.java
    ) { event: ServerTickMonitorEvent ->
        TPSBar.update(event.tickMonitor.tickTime)
        currentTick++

        var botCount = 0
        MinecraftServer.getConnectionManager().onlinePlayers.forEach { pl ->
            if (pl.username.startsWith("BOT_")) {
                botCount++
                val circCount = botCount / 10
                var angle = botCount * 36 + currentTick
                val rad = 2 + circCount * 4
                if (circCount % 2 == 0) {
                    angle = -angle
                }
                val pos = Pos(sin(angle / 180.0 * 3.1415) * rad, 40.0, cos(angle / 180.0 * 3.1415) * rad, angle % 360 + 90f, 0f)
                pl.teleport(pos)
            }
        }
    }

    MinecraftServer.getCommandManager().register(TeleportCommand())

    MinecraftServer.setBrandName("NEVERLAND (LOBBY)")

    val config = Path("neverland.properties").readLines()
        .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }

    server.start("0.0.0.0", config["lobby-port"]?.toIntOrNull() ?: 25565)
}