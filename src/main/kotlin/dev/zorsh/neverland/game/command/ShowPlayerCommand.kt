package dev.zorsh.neverland.game.command

import dev.zorsh.neverland.game.util.showToEveryone
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player

class ShowPlayerCommand : Command("showplayer") {
    init {
        defaultExecutor = CommandExecutor { sender: CommandSender, _ ->
            sender.sendMessage(Component.text("/showplayer <ник>").color(NamedTextColor.RED))
        }

        val playerArg = ArgumentType.String("игрок")
        addSyntax({ sender: CommandSender, context: CommandContext ->
            if (sender !is Player) {
                return@addSyntax
            }

            val target = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(context.get(playerArg))

            if (target == null) {
                sender.sendMessage(Component.text("/showplayer <ник>").color(NamedTextColor.RED))
            } else {
                target.showToEveryone()
            }
        }, playerArg)
    }
}