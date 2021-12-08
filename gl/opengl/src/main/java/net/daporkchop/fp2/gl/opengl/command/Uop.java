/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.command;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.opengl.command.state.CowState;
import net.daporkchop.fp2.gl.opengl.command.state.MutableState;
import net.daporkchop.fp2.gl.opengl.command.state.State;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperty;
import net.daporkchop.fp2.gl.opengl.layout.BaseBindingImpl;
import net.daporkchop.fp2.gl.opengl.shader.BaseShaderProgramImpl;
import net.daporkchop.fp2.gl.shader.BaseShaderProgram;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class Uop {
    @NonNull
    private final State state;

    public Stream<StateProperty> depends() {
        return this.dependsFirst().flatMap(property -> Stream.concat(Stream.of(property), property.depends(this.state))).distinct();
    }

    protected abstract Stream<StateProperty> dependsFirst();

    public abstract void emitCode(@NonNull CommandBufferBuilderImpl builder, @NonNull MethodVisitor mv, int apiLvtIndex);

    /**
     * @author DaPorkchop_
     */
    public static abstract class Bound extends Uop {
        protected static State toBoundState(@NonNull CowState stateIn, @NonNull BaseBindingImpl binding, @NonNull BaseShaderProgramImpl shader) {
            MutableState state = stateIn.mutableSnapshot();

            state.set(StateProperties.BOUND_PROGRAM, shader.id());
            if (binding.vao() > 0) {
                state.set(StateProperties.BOUND_VAO, binding.vao());
            }

            binding.shaderStorageBuffers().forEach(ssboBinding -> state.set(StateProperties.BOUND_SSBO[ssboBinding.bindingIndex], ssboBinding.buffer.id()));
            binding.uniformBuffers().forEach(uboBinding -> state.set(StateProperties.BOUND_UBO[uboBinding.bindingIndex], uboBinding.buffer.id()));

            binding.textures().forEach(textureBinding -> state.set(StateProperties.BOUND_TEXTURE[textureBinding.unit].get(textureBinding.target), textureBinding.id));

            return state.immutableSnapshot();
        }

        protected final List<StateProperty> depends;

        public Bound(@NonNull CowState state, @NonNull BaseBindingImpl binding, @NonNull BaseShaderProgramImpl shader) {
            super(toBoundState(state, binding, shader));

            ImmutableList.Builder<StateProperty> depends = ImmutableList.builder();
            this.buildDependsFirst(depends, binding);
            this.depends = depends.build();
        }

        protected void buildDependsFirst(@NonNull ImmutableList.Builder<StateProperty> depends, @NonNull BaseBindingImpl binding) {
            depends.add(StateProperties.BOUND_PROGRAM);
            if (binding.vao() > 0) {
                depends.add(StateProperties.BOUND_VAO);
            }

            binding.shaderStorageBuffers().forEach(ssboBinding -> depends.add(StateProperties.BOUND_SSBO[ssboBinding.bindingIndex]));
            binding.uniformBuffers().forEach(uboBinding -> depends.add(StateProperties.BOUND_UBO[uboBinding.bindingIndex]));

            binding.textures().forEach(textureBinding -> depends.add(StateProperties.BOUND_TEXTURE[textureBinding.unit].get(textureBinding.target)));
        }

        @Override
        protected Stream<StateProperty> dependsFirst() {
            return this.depends.stream();
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static abstract class Draw extends Bound {
        public Draw(@NonNull CowState state, @NonNull DrawBinding binding, @NonNull DrawShaderProgram shader) {
            super(state, (BaseBindingImpl) binding, (BaseShaderProgramImpl) shader);
        }

        @Override
        protected Stream<StateProperty> dependsFirst() {
            return Stream.concat(super.dependsFirst(), Stream.of(StateProperties.FIXED_FUNCTION_DRAW_PROPERTIES));
        }
    }
}
