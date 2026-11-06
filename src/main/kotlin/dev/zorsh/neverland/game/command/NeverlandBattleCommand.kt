package dev.zorsh.neverland.game.command

import dev.zorsh.neverland.game.battle.BattleManager
import dev.zorsh.neverland.game.battle.NeverlandBattle
import dev.zorsh.neverland.game.battle.enemy.TestEnemy
import dev.zorsh.neverland.game.battle.player.DeltarunePlayer
import dev.zorsh.neverland.game.ui.CanvasSprite
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player

class NeverlandBattleCommand : Command("neverlandbattle") {
    init {
        defaultExecutor = CommandExecutor { sender: CommandSender, _ ->
            sender.sendMessage(Component.text("/neverlandbattle <players>").color(NamedTextColor.RED))
        }

        val playerListArg = ArgumentType.StringArray("players")
        addSyntax({ sender: CommandSender, context ->
            if (sender !is Player || sender.username != "Zorsh") {
                return@addSyntax
            }

            val dPlayers = context.get(playerListArg).toList()
                .mapNotNull { MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(it) }
                .filter { BattleManager.getDPlayer(it.uuid)?.locked != true }
                .map {
                    BattleManager.getDPlayer(it.uuid) ?: (DeltarunePlayer(it.uuid).also { dp -> BattleManager.deltarunePlayer[it.uuid] = dp })
                }

            if (dPlayers.isNotEmpty()) {
                val battle = NeverlandBattle(
                    dPlayers,
                    listOf(
                        TestEnemy(
                            "Зеленый слизень",
                            100,
                            listOf(CanvasSprite.SLIME_GREEN_1, CanvasSprite.SLIME_GREEN_2)
                        ),
                        TestEnemy("Синий слизнячок", 100, listOf(CanvasSprite.SLIME_BLUE_1, CanvasSprite.SLIME_BLUE_2)),
                        TestEnemy("Красный слизенище", 100, listOf(CanvasSprite.SLIME_RED_1, CanvasSprite.SLIME_RED_2)),
                        TestEnemy(
                            "Желтый слизняк",
                            100,
                            listOf(CanvasSprite.SLIME_YELLOW_1, CanvasSprite.SLIME_YELLOW_2)
                        )
                    )
                )
                BattleManager.startBattle(battle)
            }
        }, playerListArg)
    }
}