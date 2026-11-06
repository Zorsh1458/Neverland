package dev.zorsh.neverland.game

import kotlin.random.Random

class Neverland {
    companion object {
        val random = Random(1458)
        lateinit var config: Map<String, String>
    }
}