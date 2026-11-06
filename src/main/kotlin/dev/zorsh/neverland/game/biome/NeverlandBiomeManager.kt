package dev.zorsh.neverland.game.biome

import net.minestom.server.MinecraftServer
import net.minestom.server.color.Color
import net.minestom.server.registry.RegistryKey
import net.minestom.server.world.biome.Biome
import net.minestom.server.world.biome.BiomeEffects

class NeverlandBiomeManager {
    companion object {
        lateinit var testBiome: RegistryKey<Biome>

        fun registerBiomes() {
            val registry = MinecraftServer.getBiomeRegistry()

            val effects = BiomeEffects.builder()
                .skyColor(Color(0, 0, 0))
                .fogColor(Color(0, 0, 0))
                .foliageColor(Color(24, 255, 48))
                .waterColor(Color(0, 0, 0))
                .waterFogColor(Color(0, 0, 0))
                .grassColor(Color(24, 255, 48))
                .build()

            val biome = Biome.builder()
                .effects(effects)
                .downfall(0f)
                .temperature(1f)
                .hasPrecipitation(false)
                .build()

            testBiome = registry.register("neverland:moon", biome)
        }
    }
}