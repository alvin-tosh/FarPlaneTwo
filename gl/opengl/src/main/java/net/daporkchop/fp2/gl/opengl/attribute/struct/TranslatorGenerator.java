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

package net.daporkchop.fp2.gl.opengl.attribute.struct;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.translator.InterleavedStructTranslator;
import net.daporkchop.fp2.gl.opengl.attribute.struct.translator.StructTranslator;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.concurrent.ExecutionException;

import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
public class TranslatorGenerator {
    protected final Cache<StructLayout<?>, StructTranslator<?, ?>> translatorCache = CacheBuilder.newBuilder()
            .weakValues()
            .build();

    @SneakyThrows(ExecutionException.class)
    public <S> InterleavedStructTranslator<S> getInterleaved(@NonNull InterleavedStructLayout<S> layout) {
        return uncheckedCast(this.translatorCache.get(layout, () -> this.generateInterleaved(layout)));
    }

    private <S> InterleavedStructTranslator<S> generateInterleaved(@NonNull InterleavedStructLayout<S> layout) throws Exception {
        String baseClassName = Type.getInternalName(InterleavedStructTranslator.class);
        String className = baseClassName + '$' + layout.layoutName() + '$' + Type.getInternalName(layout.structInfo().clazz()).replace("/", "__");
        String structName = Type.getInternalName(layout.structInfo().clazz());

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        {
            SignatureWriter signature = new SignatureWriter();
            signature.visitSuperclass();
            signature.visitClassType(baseClassName);
            signature.visitTypeArgument('=');
            signature.visitClassType(structName);
            signature.visitEnd();
            signature.visitEnd();

            writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, signature.toString(), baseClassName, null);
        }

        { //constructor
            SignatureWriter signature = new SignatureWriter();
            signature.visitParameterType();
            signature.visitClassType(Type.getInternalName(InterleavedStructLayout.class));
            signature.visitEnd();
            signature.visitReturnType();
            signature.visitBaseType('V');

            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", signature.toString(), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, baseClassName, "<init>", signature.toString(), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //void copy(Object struct, Object dstBase, long dstOffset)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copy", "(Ljava/lang/Object;Ljava/lang/Object;J)V", null, null);

            //make sure struct can be cast to requested type
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, structName);
            mv.visitVarInsn(ASTORE, 1);

            //copy each member type
            for (int i = 0; i < layout.structInfo().members.size(); i++) {
                StructMember<S> member = layout.structInfo().members.get(i);
                member.storeStageOutput(mv, layout.unpacked() ? member.unpackedStage : member.packedStage, 1, 2, 3, layout.memberOffsets()[i]);
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        writer.visitEnd();

        if (true) {
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get(className.replace('/', '-') + ".class"), writer.toByteArray());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }

        Class<InterleavedStructTranslator<S>> clazz = uncheckedCast(ClassloadingUtils.defineHiddenClass(InterleavedStructTranslator.class.getClassLoader(), writer.toByteArray()));
        return clazz.getConstructor(InterleavedStructLayout.class).newInstance(layout);
    }
}
