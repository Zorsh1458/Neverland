package dev.zorsh.neverland.lobby.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player


class TeleportCommand : Command("teleport", "tp") {
    init {
        defaultExecutor = CommandExecutor { sender: CommandSender, _ ->
            sender.sendMessage(Component.text("/teleport <ник>").color(NamedTextColor.RED))
        }

        val gameModeArg = ArgumentType.String("Ник игрока")
        addSyntax({ sender: CommandSender, context: CommandContext ->
            if (sender !is Player) {
                return@addSyntax
            }

            val pl = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(context.get(gameModeArg))
            if (pl == null) {
                sender.sendMessage(Component.text("Этого игрока не существует").color(NamedTextColor.RED))
                return@addSyntax
            }

            sender.teleport(pl.position)
        }, gameModeArg)
    }
}