package dev.zorsh.neverland.game.console

import dev.zorsh.neverland.game.Neverland
import net.minestom.server.MinecraftServer
import net.minestom.server.network.packet.server.common.TransferPacket

class ConsoleCommandListener {
    companion object {
        var onStop: (() -> Unit)? = null

        @JvmStatic
        fun onStop(handler: () -> Unit) {
            onStop = handler
        }

        @JvmStatic
        fun start() {
            while (true) {
                try {
                    val command = readln().trim()
                    if (command.isEmpty()) continue

                    if (command == "restart" || command == "stop") {
                        onStop?.invoke()

                        val lobbyPort = Neverland.config["lobby-port"]?.toIntOrNull() ?: continue
                        val serverIp = Neverland.config["server-ip"] ?: continue

                        MinecraftServer.getInstanceManager().instances.forEach {
                            it.players.forEach { player ->
                                player.playerConnection.sendPacket(
                                    TransferPacket(serverIp, lobbyPort)
                                )
                            }
                        }
                    }
                } catch (_: RuntimeException) {
                    break
                }
            }
        }
    }
}