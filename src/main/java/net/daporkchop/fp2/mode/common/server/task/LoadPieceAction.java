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

package net.daporkchop.fp2.mode.common.server.task;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.mode.api.CompressedPiece;
import net.daporkchop.fp2.mode.api.piece.IFarPieceBuilder;
import net.daporkchop.fp2.mode.common.server.AbstractFarWorld;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.IFarPos;

import java.util.concurrent.Callable;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Not really a task, but used by {@link AbstractFarWorld} to load a piece from disk and create it if absent.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class LoadPieceAction<POS extends IFarPos, P extends IFarPiece, B extends IFarPieceBuilder> implements Callable<CompressedPiece<POS, P, B>> {
    @NonNull
    protected final AbstractFarWorld<POS, P, B> world;
    @NonNull
    protected final POS pos;

    @Override
    public CompressedPiece<POS, P, B> call() throws Exception {
        CompressedPiece<POS, P, B> piece = this.world.storage().load(this.pos);
        if (piece == null) {
            //piece doesn't exist on disk, let's make a new one!
            return new CompressedPiece<>(this.pos);
        }
        return piece;
    }
}