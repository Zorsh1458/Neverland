package dev.zorsh.neverland.game.command

import dev.zorsh.neverland.game.chess.ChessManager
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.entity.Player

class ChessCommand : Command("chess") {
    init {
        defaultExecutor = CommandExecutor { sender: CommandSender, _ ->
            if (sender is Player) {
                ChessManager.startChess(sender)
            }
        }
    }
}