package dev.zorsh.neverland.game.battle

import dev.zorsh.neverland.game.battle.player.DeltarunePlayer
import dev.zorsh.neverland.game.command.HidePlayerCommand
import dev.zorsh.neverland.game.command.NeverlandBattleCommand
import dev.zorsh.neverland.game.command.ShowPlayerCommand
import dev.zorsh.neverland.game.listener.NeverlandPlayerInputListener
import dev.zorsh.neverland.game.logger.Logger
import dev.zorsh.neverland.game.util.InputHolder
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.listener.PlayerInputListener
import net.minestom.server.listener.manager.PacketPlayListenerConsumer
import net.minestom.server.network.packet.client.play.ClientInputPacket
import java.util.*

class BattleManager {
    companion object {
        private var battlesList = mutableMapOf<UUID, INeverlandBattle>()

        @JvmStatic
        fun destroyAllBattles() {
            for ((_, battle) in battlesList) {
                battle.destroyBattle()
            }
            battlesList = mutableMapOf()
        }

        @JvmStatic
        fun startBattle(battle: INeverlandBattle) {
            val uuid = UUID.randomUUID()
            battlesList[uuid] = battle
            battle.setUUID(uuid)
            for (dPlayer in battle.getBattleInitialPlayers()) {
                dPlayer.myBattleUUID = uuid
            }
            battle.startBattle {
                battlesList.remove(uuid)
            }
        }

        @JvmStatic
        fun hasBattle(uuid: UUID) = battlesList.containsKey(uuid)

        @JvmStatic
        fun getBattle(uuid: UUID) = battlesList[uuid]

        @JvmStatic
        fun getAllBattles() = battlesList.map { it.key }

        var deltarunePlayer = mutableMapOf<UUID, DeltarunePlayer>()

        @JvmStatic
        fun getDPlayer(uuid: UUID): DeltarunePlayer? {
            return deltarunePlayer[uuid]
        }

        fun enable() {
            val globalEventHandler = MinecraftServer.getGlobalEventHandler()
            globalEventHandler.addListener(PlayerDisconnectEvent::class.java) { event ->
                val id = event.player.uuid
                deltarunePlayer.remove(id)
            }

            MinecraftServer.getPacketListenerManager().setPlayListener(
                ClientInputPacket::class.java,
                NeverlandPlayerInputListener::listener
            )

            MinecraftServer.getCommandManager().register(NeverlandBattleCommand())
            MinecraftServer.getCommandManager().register(HidePlayerCommand())
            MinecraftServer.getCommandManager().register(ShowPlayerCommand())
            Logger.info("BattleManager enabled!")
        }

        fun disable() {
            Logger.info("Battles: ${getAllBattles()}")
            destroyAllBattles()
            Logger.info("BattleManager disabled!")
        }
    }
}