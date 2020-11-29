/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.util.compat.vanilla.biome;

import lombok.NonNull;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

/**
 * A faster alternative to {@link BiomeProvider}.
 * <p>
 * Note that all methods use XZ coordinate order instead of vanilla's ZX.
 *
 * @author DaPorkchop_
 */
public interface IBiomeProvider {
    /**
     * Generates the biome at the given position.
     *
     * @param blockX the X coordinate of the biome to generate
     * @param blockZ the Z coordinate of the biome to generate
     * @return the generated biome
     */
    Biome biome(int blockX, int blockZ);

    /**
     * Generates the ID of the biome at the given position.
     *
     * @param blockX the X coordinate of the biome to generate
     * @param blockZ the Z coordinate of the biome to generate
     * @return the generated biome's ID
     */
    int biomeId(int blockX, int blockZ);

    /**
     * Generates the biomes in the given region.
     *
     * @param arr    the array to store the generated biomes in
     * @param blockX the X coordinate of the region to generate
     * @param blockZ the Z coordinate of the region to generate
     * @param sizeX  the size of the region along the X axis
     * @param sizeZ  the size of the region along the Z axis
     * @throws IllegalArgumentException if the given array is too small to fit the region
     */
    void biomes(@NonNull Biome[] arr, int blockX, int blockZ, int sizeX, int sizeZ);

    /**
     * Generates the IDs of the biomes in the given region.
     *
     * @param arr    the array to store the generated biomes' IDs in
     * @param blockX the X coordinate of the region to generate
     * @param blockZ the Z coordinate of the region to generate
     * @param sizeX  the size of the region along the X axis
     * @param sizeZ  the size of the region along the Z axis
     * @throws IllegalArgumentException if the given array is too small to fit the region
     */
    void biomeIds(@NonNull byte[] arr, int blockX, int blockZ, int sizeX, int sizeZ);

    /**
     * Generates the IDs of the generation (low-resolution) biomes in the given region.
     *
     * @param arr   the array to store the generated biomes' IDs in
     * @param x     the X coordinate of the region to generate
     * @param z     the Z coordinate of the region to generate
     * @param sizeX the size of the region along the X axis
     * @param sizeZ the size of the region along the Z axis
     * @throws IllegalArgumentException if the given array is too small to fit the region
     */
    void biomeIdsForGeneration(@NonNull int[] arr, int x, int z, int sizeX, int sizeZ);
}
