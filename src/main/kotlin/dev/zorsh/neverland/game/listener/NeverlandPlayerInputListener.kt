package dev.zorsh.neverland.game.listener

import dev.zorsh.neverland.game.battle.BattleManager.Companion.getDPlayer
import dev.zorsh.neverland.game.util.InputHolder
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.client.play.ClientInputPacket

class NeverlandPlayerInputListener {
    companion object {
        fun listener(packet: ClientInputPacket, player: Player) {
            player.refreshInput(
                packet.forward(), packet.backward(),
                packet.left(), packet.right(),
                packet.jump(),
                packet.shift(),
                packet.sprint()
            )

            getDPlayer(player.uuid)?.updateInputs(
                InputHolder(
                    packet.left(),
                    packet.right(),
                    packet.forward(),
                    packet.backward(),
                    packet.jump(),
                    packet.shift(),
                    packet.sprint()
                )
            )
        }
    }
}