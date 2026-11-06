package dev.zorsh.neverland.game.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player


class GameModeCommand : Command("gamemode") {
    init {
        defaultExecutor = CommandExecutor { sender: CommandSender, _ ->
            sender.sendMessage(Component.text("/gamemode <creative|survival|adventure|spectator>").color(NamedTextColor.RED))
        }

        val gameModeArg = ArgumentType.String("game mode")
        addSyntax({ sender: CommandSender, context: CommandContext ->
            if (sender !is Player) {
                return@addSyntax
            }
            when (context.get(gameModeArg)) {
                "survival" -> {
                    sender.gameMode = GameMode.SURVIVAL
                }
                "creative" -> {
                    sender.gameMode = GameMode.CREATIVE
                }
                "adventure" -> {
                    sender.gameMode = GameMode.ADVENTURE
                }
                "spectator" -> {
                    sender.gameMode = GameMode.SPECTATOR
                }
                else -> {
                    sender.sendMessage(Component.text("/gamemode <creative|survival|adventure|spectator>").color(NamedTextColor.RED))
                }
            }
        }, gameModeArg)
    }
}