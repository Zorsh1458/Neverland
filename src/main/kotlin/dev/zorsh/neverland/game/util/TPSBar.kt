package dev.zorsh.neverland.game.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import kotlin.math.max
import kotlin.math.min


class TPSBar {
    companion object {
        fun update(mspt: Double) {
            val tps = min(20.0, 1000.0 / max(1.0, mspt))

            val actionBarContent = Component.text()
                .append(Component.text("TPS: ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.1f", tps), getTpsColor(tps)))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("MSPT: ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.1f", mspt), getMsptColor(mspt)))
                .build()

            for (player in MinecraftServer.getConnectionManager().onlinePlayers) {
                player.sendActionBar(actionBarContent)
            }
        }

        private fun getTpsColor(tps: Double): NamedTextColor {
            if (tps >= 18.0) return NamedTextColor.GREEN
            if (tps >= 15.0) return NamedTextColor.YELLOW
            return NamedTextColor.RED
        }

        private fun getMsptColor(mspt: Double): NamedTextColor {
            if (mspt <= 40.0) return NamedTextColor.GREEN
            if (mspt <= 50.0) return NamedTextColor.YELLOW
            return NamedTextColor.RED
        }
    }
}