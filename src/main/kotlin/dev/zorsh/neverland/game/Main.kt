package dev.zorsh.neverland.game

import dev.zorsh.neverland.game.battle.BattleManager
import dev.zorsh.neverland.game.biome.NeverlandBiomeManager
import dev.zorsh.neverland.game.chess.ChessManager
import dev.zorsh.neverland.game.chunkloader.MoonWorldLoader
import dev.zorsh.neverland.game.command.ChessCommand
import dev.zorsh.neverland.game.command.GameModeCommand
import dev.zorsh.neverland.game.console.ConsoleCommandListener
import dev.zorsh.neverland.game.logger.Logger
import dev.zorsh.neverland.game.util.TPSBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.server.ServerTickMonitorEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.network.packet.server.play.TimeUpdatePacket
import net.minestom.server.world.DimensionType
import kotlin.io.path.Path
import kotlin.io.path.readLines


fun main() {
    val server = MinecraftServer.init()

    NeverlandBiomeManager.registerBiomes()

    val instanceManager = MinecraftServer.getInstanceManager()

    val lobbyDimension = MinecraftServer.getDimensionTypeRegistry()
        .register(
            "neverland:main", DimensionType.builder()
                .ambientLight(0.0f)
                .hasSkylight(false)
                .build()
        )
    val instanceContainer = instanceManager.createInstanceContainer(lobbyDimension)
    instanceContainer.chunkLoader = MoonWorldLoader()

    val globalEventHandler = MinecraftServer.getGlobalEventHandler()
    globalEventHandler.addListener(
        AsyncPlayerConfigurationEvent::class.java
    ) { event: AsyncPlayerConfigurationEvent ->
        val player = event.player
        event.spawningInstance = instanceContainer
        player.respawnPoint = Pos(1140.0, 30.0, 70.0)
    }
    globalEventHandler.addListener(
        PlayerSpawnEvent::class.java
    ) { event: PlayerSpawnEvent ->
        val player = event.player
        player.gameMode = GameMode.CREATIVE
        Logger.info("${player.username} присоединился к игре")
        instanceContainer.players.forEach { pl ->
            pl.sendMessage(player.name.append(Component.text(" присоединился к игре")).color(NamedTextColor.YELLOW))
        }
    }
    globalEventHandler.addListener(
        PlayerDisconnectEvent::class.java
    ) { event: PlayerDisconnectEvent ->
        val player = event.player
        Logger.info("${player.username} вышел из игры")
        instanceContainer.players.forEach { pl ->
            pl.sendMessage(player.name.append(Component.text(" вышел из игры")).color(NamedTextColor.YELLOW))
        }
    }
    globalEventHandler.addListener(
        PlayerBlockInteractEvent::class.java
    ) { event: PlayerBlockInteractEvent ->
        event.isCancelled = true
    }

    var currentTime = 0L
    globalEventHandler.addListener(
        ServerTickMonitorEvent::class.java
    ) { event: ServerTickMonitorEvent ->
        TPSBar.update(event.tickMonitor.tickTime)
        currentTime = (currentTime + 1) % 20L
        MinecraftServer.getConnectionManager().onlinePlayers.forEach { pl ->
            pl.sendPacket(TimeUpdatePacket(currentTime, 6000L, false))
        }
    }

    MinecraftServer.getCommandManager().register(GameModeCommand())

    ChessManager.registerListener()
    MinecraftServer.getCommandManager().register(ChessCommand())

    MojangAuth.init()

    MinecraftServer.setBrandName("NEVERLAND (GAME)")

    val config = Path("neverland.properties").readLines()
        .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }

    server.start("0.0.0.0", config["game-port"]?.toIntOrNull() ?: 25565)

    BattleManager.enable()

    ConsoleCommandListener.onStop {
        BattleManager.disable()
    }

    ConsoleCommandListener.start()
}