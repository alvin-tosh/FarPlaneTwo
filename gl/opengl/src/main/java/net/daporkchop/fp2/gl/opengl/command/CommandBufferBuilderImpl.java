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
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import net.daporkchop.fp2.gl.command.CommandBuffer;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.command.Comparison;
import net.daporkchop.fp2.gl.command.FramebufferLayer;
import net.daporkchop.fp2.gl.command.StencilOperation;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.binding.DrawMode;
import net.daporkchop.fp2.gl.draw.command.DrawList;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.OpenGLConstants;
import net.daporkchop.fp2.gl.opengl.draw.binding.DrawBindingImpl;
import net.daporkchop.fp2.gl.opengl.draw.command.DrawListImpl;
import net.daporkchop.fp2.gl.opengl.draw.shader.DrawShaderProgramImpl;
import net.daporkchop.fp2.gl.opengl.layout.BaseBindingImpl;
import net.daporkchop.fp2.gl.opengl.shader.BaseShaderProgramImpl;
import net.daporkchop.lib.common.misc.string.PStrings;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * OpenGL implementation of {@link CommandBuffer} which generates code using ASM.
 *
 * @author DaPorkchop_
 */
public class CommandBufferBuilderImpl implements CommandBufferBuilder {
    private static final String CLASS_NAME = getInternalName(CommandBufferImpl.class) + '0';

    protected final OpenGL gl;

    protected final ClassWriter writer;

    protected final MethodVisitor ctorVisitor;
    protected final MethodVisitor codeVisitor;

    protected final String apiFieldName;
    protected final int apiLvtIndex;

    protected final BitSet lvtAllocationTable = new BitSet();
    protected final List<Object> fieldValues = new ArrayList<>();

    protected final ImmutableList.Builder<Uop> uops = ImmutableList.builder();
    protected State state = State.DEFAULT_STATE;

    protected BaseBindingImpl binding;
    protected BaseShaderProgramImpl shader;

    public CommandBufferBuilderImpl(@NonNull OpenGL gl) {
        this.gl = gl;

        this.writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        this.writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, CLASS_NAME, null, getInternalName(CommandBufferImpl.class), null);

        this.ctorVisitor = this.writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(List.class), getType(List.class)), null, null);
        this.ctorVisitor.visitCode();
        this.ctorVisitor.visitVarInsn(ALOAD, 0);
        this.ctorVisitor.visitVarInsn(ALOAD, 1);
        this.ctorVisitor.visitMethodInsn(INVOKESPECIAL, getInternalName(CommandBufferImpl.class), "<init>", getMethodDescriptor(VOID_TYPE, getType(List.class)), false);

        this.apiFieldName = this.makeField(getType(GLAPI.class), gl.api());

        this.codeVisitor = this.writer.visitMethod(ACC_PUBLIC | ACC_FINAL, "execute", getMethodDescriptor(VOID_TYPE), null, null);
        this.codeVisitor.visitCode();
        this.lvtAllocationTable.set(0);

        this.codeVisitor.visitVarInsn(ALOAD, 0);
        this.codeVisitor.visitFieldInsn(GETFIELD, CLASS_NAME, this.apiFieldName, getDescriptor(GLAPI.class));
        this.codeVisitor.visitVarInsn(ASTORE, this.apiLvtIndex = this.allocateLocalVariable());
    }

    protected String makeField(@NonNull Type type, @NonNull Object value) {
        //assign a new field name
        int index = this.fieldValues.size();
        this.fieldValues.add(value);
        String name = PStrings.fastFormat("field_%04x", index);

        //define the field
        this.writer.visitField(ACC_PRIVATE | ACC_FINAL, name, type.getDescriptor(), null, null).visitEnd();

        //set the field value in the constructor
        this.ctorVisitor.visitVarInsn(ALOAD, 0); //this
        this.ctorVisitor.visitVarInsn(ALOAD, 2); //list
        this.ctorVisitor.visitLdcInsn(index); //index
        this.ctorVisitor.visitMethodInsn(INVOKEINTERFACE, getInternalName(List.class), "get", getMethodDescriptor(getType(Object.class), INT_TYPE), true); //list.get(index)
        this.ctorVisitor.visitTypeInsn(CHECKCAST, type.getInternalName()); //(type)
        this.ctorVisitor.visitFieldInsn(PUTFIELD, CLASS_NAME, name, type.getDescriptor()); //this.name = (type) list.get(index);

        return name;
    }

    protected int allocateLocalVariable() {
        int lvtIndex = this.lvtAllocationTable.nextClearBit(0);
        this.lvtAllocationTable.set(lvtIndex);
        return lvtIndex;
    }

    protected void releaseLocalVariable(int lvtIndex) {
        this.lvtAllocationTable.clear(lvtIndex);
    }

    protected void bind(@NonNull BaseBindingImpl binding) {
        checkArg(binding.gl() == this.gl, "binding belongs to another context");

        this.state = this.state.withVao(binding.vao());

        //bind SSBOs
        binding.shaderStorageBuffers().forEach(ssboBinding -> this.state = this.state.withShaderStorageBuffer(ssboBinding.bindingIndex, ssboBinding.buffer.id()));
        binding.uniformBuffers().forEach(uniformBinding -> this.state = this.state.withUniformBuffer(uniformBinding.bindingIndex, uniformBinding.buffer.id()));

        //bind textures
        /*binding.textures().forEach(textureBinding -> {
            //switch texture units
            this.loadGLAPI();
            this.codeVisitor.visitLdcInsn(GL_TEXTURE0 + textureBinding.unit);
            this.codeVisitor.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glActiveTexture", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);

            //actually bind the texture
            this.loadGLAPI();
            this.codeVisitor.visitLdcInsn(textureBinding.target.target());
            this.codeVisitor.visitLdcInsn(textureBinding.id);
            this.codeVisitor.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBindTexture", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE), true);
        });*/

        this.binding = binding;
    }

    protected void bind(@NonNull BaseShaderProgramImpl shader) {
        checkArg(shader.gl() == this.gl, "shader belongs to another context");

        this.state = this.state.withProgram(shader.id());
    }

    @Override
    public CommandBufferBuilder framebufferClear(@NonNull FramebufferLayer... layers) {
        //TODO
        return this;
    }

    @Override
    public CommandBufferBuilder stencilEnable() {
        this.state = this.state.withStencil(true);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilDisable() {
        this.state = this.state.withStencil(false);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilWriteMask(int writeMask) {
        this.state = this.state.withStencilWriteMask(writeMask);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilCompareMask(int compareMask) {
        this.state = this.state.withStencilCompareMask(compareMask);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilReference(int reference) {
        this.state = this.state.withStencilReference(reference);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilCompare(@NonNull Comparison comparison) {
        this.state = this.state.withStencilCompare(comparison);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilOperation(@NonNull StencilOperation fail, @NonNull StencilOperation pass, @NonNull StencilOperation depthFail) {
        this.state = this.state.withStencilOperationFail(fail).withStencilOperationPass(pass).withStencilOperationDepthFail(depthFail);
        return this;
    }

    @Override
    public CommandBufferBuilder drawArrays(@NonNull DrawBinding _binding, @NonNull DrawShaderProgram _shader, @NonNull DrawMode mode, int first, int count) {
        this.bind((DrawBindingImpl) _binding);
        this.bind((DrawShaderProgramImpl) _shader);

        this.uops.add(new Uop(this.state) {
            @Override
            public void emitCode(@NonNull CommandBufferBuilderImpl builder, @NonNull State lastState, @NonNull MethodVisitor mv, int apiLvtIndex) {
                super.emitCode(builder, lastState, mv, apiLvtIndex);

                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GLEnumUtil.from(mode));
                mv.visitLdcInsn(first);
                mv.visitLdcInsn(count);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glDrawArrays", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
            }
        });
        return this;
    }

    @Override
    public CommandBufferBuilder drawList(@NonNull DrawShaderProgram _shader, @NonNull DrawMode _mode, @NonNull DrawList<?> _list) {
        DrawListImpl<?, ?> list = (DrawListImpl<?, ?>) _list;
        this.bind((DrawBindingImpl) list.binding());
        this.bind((DrawShaderProgramImpl) _shader);
        return this;
    }

    @Override
    public CommandBufferBuilder execute(@NonNull CommandBuffer buffer) {
        this.uops.addAll(((CommandBufferImpl) buffer).uops);
        return this;
    }

    @Override
    @SneakyThrows({ IllegalAccessException.class, InstantiationException.class, InvocationTargetException.class, NoSuchMethodException.class })
    public CommandBuffer build() {
        List<Uop> uops = this.uops.build();
        {
            State state = State.DEFAULT_STATE;
            for (Uop uop : uops) {
                uop.emitCode(this, state, this.codeVisitor, this.apiLvtIndex);
                state = uop.state();
            }
        }

        this.ctorVisitor.visitInsn(RETURN);
        this.ctorVisitor.visitMaxs(0, 0);

        this.codeVisitor.visitInsn(RETURN);
        this.codeVisitor.visitMaxs(0, 0);

        this.writer.visitEnd();

        if (false) {
            try {
                Files.write(Paths.get("CommandBufferImpl.class"), this.writer.toByteArray());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        Class<? extends CommandBuffer> clazz = uncheckedCast(ClassloadingUtils.defineHiddenClass(CommandBufferBuilderImpl.class.getClassLoader(), this.writer.toByteArray()));
        return clazz.getDeclaredConstructor(List.class, List.class).newInstance(uops, this.fieldValues);
    }
}
