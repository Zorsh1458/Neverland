package dev.zorsh.neverland.game.chunkloader

import dev.zorsh.neverland.game.biome.NeverlandBiomeManager
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.IChunkLoader
import net.minestom.server.instance.Instance
import net.minestom.server.instance.anvil.AnvilLoader
import kotlin.io.path.Path

class MoonWorldLoader : IChunkLoader {
    val anvilLoader = AnvilLoader(Path("worlds/SuperflatTest"))

    override fun loadChunk(
        instance: Instance,
        chunkX: Int,
        chunkZ: Int,
    ): Chunk? {
        var chunk = anvilLoader.loadChunk(instance, chunkX, chunkZ)
        if (chunkX !in 66..75 || chunkZ !in -3..9) {
            chunk = instance.chunkSupplier.createChunk(instance, chunkX, chunkZ)
        }

        val dim = MinecraftServer.getDimensionTypeRegistry()[instance.dimensionType]
        if (dim != null) {
            for (x in 0 until 4) {
                for (y in dim.minY() until dim.maxY()) {
                    for (z in 0 until 4) {
                        chunk?.setBiome(x * 4, y, z * 4, NeverlandBiomeManager.testBiome)
                    }
                }
            }
        }

        return chunk
    }

    override fun saveChunk(chunk: Chunk) {}
}