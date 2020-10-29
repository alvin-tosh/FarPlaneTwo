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

package net.daporkchop.fp2.mode.common.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.stream.Stream;

/**
 * Bakes render data based on piece data.
 *
 * @author DaPorkchop_
 */
public interface IFarRenderBaker<POS extends IFarPos, P extends IFarPiece> {
    /**
     * Gets the primitive type used for indices generated by this render baker.
     * <p>
     * Must be one of:
     * - {@link GL11#GL_UNSIGNED_BYTE}
     * - {@link GL11#GL_UNSIGNED_SHORT}
     * - {@link GL11#GL_UNSIGNED_INT}
     *
     * @return the primitive type used for indices
     */
    int indexType();

    /**
     * @return the number of vertex attributes
     */
    int vertexAttributes();

    /**
     * Assigns the vertex attributes to their intended values.
     * <p>
     * All buffer objects will already be correctly bound when this method is called.
     * <p>
     * Implementations may use methods such as {@link GL20#glVertexAttribPointer(int, int, int, boolean, int, long)} or
     * {@link GL30#glVertexAttribIPointer(int, int, int, int, long)}.
     */
    void assignVertexAttributes();

    /**
     * Gets the positions of all the pieces whose baked contents are affected by the content of the given piece.
     * <p>
     * The returned {@link Stream} must be sequential!
     *
     * @param srcPos the position of the piece
     * @return the positions of all the pieces whose contents are affected by the content of the given piece
     */
    Stream<POS> bakeOutputs(@NonNull POS srcPos);

    /**
     * Gets the positions of all the pieces needed to bake the given piece.
     * <p>
     * The returned {@link Stream} must be sequential and in sorted order!
     *
     * @param dstPos the position of the piece to generate
     * @return the positions of all the pieces needed to create the given piece
     */
    Stream<POS> bakeInputs(@NonNull POS dstPos);

    /**
     * @return the estimated capacity required for the vertices buffer
     */
    int estimatedVerticesBufferCapacity();

    /**
     * @return the estimated capacity required for the indices buffer
     */
    int estimatedIndicesBufferCapacity();

    /**
     * Takes the content of the given pieces and bakes it into a {@link ByteBuf} for rendering.
     *
     * @param dstPos             the position of the piece to bake
     * @param srcs               an array containing the input pieces. Pieces are in the same order as provided by the {@link Stream} returned by
     *                           {@link #bakeInputs(IFarPos)}, and will be {@code null} if not locally available. The piece at dstPos is guaranteed
     *                           to be present (if requested by {@link #bakeInputs(IFarPos)})
     * @param vertices           the {@link ByteBuf} to write baked data to
     * @param opaqueIndices      the {@link ByteBuf} to write indices for opaque geometry to
     * @param cutoutIndices      the {@link ByteBuf} to write indices for cutout geometry to
     * @param translucentIndices the {@link ByteBuf} to write indices for translucent geometry to
     */
    void bake(@NonNull POS dstPos, @NonNull P[] srcs, @NonNull ByteBuf vertices, @NonNull ByteBuf opaqueIndices, @NonNull ByteBuf cutoutIndices, @NonNull ByteBuf translucentIndices);
}