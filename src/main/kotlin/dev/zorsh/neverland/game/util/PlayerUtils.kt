package dev.zorsh.neverland.game.util

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player

fun Player.hideFromEveryone() {
    isAutoViewable = false
    MinecraftServer.getConnectionManager().onlinePlayers.filter { it.uuid != this.uuid }.forEach { other ->
        removeViewer(other)
    }
}

fun Player.showToEveryone() {
    MinecraftServer.getConnectionManager().onlinePlayers.filter { it.uuid != this.uuid }.forEach { other ->
        addViewer(other)
    }
    isAutoViewable = true
}