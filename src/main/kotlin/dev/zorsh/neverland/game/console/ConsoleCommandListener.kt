package dev.zorsh.neverland.game.console

import net.minestom.server.MinecraftServer
import net.minestom.server.network.packet.server.common.TransferPacket
import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.text.substringAfter
import kotlin.text.substringBefore

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

                        val config = Path("neverland.properties").readLines()
                            .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
                        val lobbyPort = config["lobby-port"]?.toIntOrNull() ?: continue
                        val serverIp = config["server-ip"] ?: continue

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