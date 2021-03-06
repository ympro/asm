// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.objectweb.asm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.test.AsmTest;

/**
 * CheckClassAdapter tests.
 *
 * @author Eric Bruneton
 */
public class CheckClassAdapterTest extends AsmTest implements Opcodes {

  @Test
  public void testCheckClassVisitor() throws Exception {
    PrintStream err = System.err;
    PrintStream out = System.out;
    System.setErr(new PrintStream(new ByteArrayOutputStream()));
    System.setOut(new PrintStream(new ByteArrayOutputStream()));
    try {
      String s = getClass().getName();
      CheckClassAdapter.main(new String[0]);
      CheckClassAdapter.main(new String[] {s});
      CheckClassAdapter.main(new String[] {"java.lang.Object"});
    } finally {
      System.setErr(err);
      System.setOut(out);
    }
  }

  @Test
  public void testVerifyValidClass() throws Exception {
    ClassReader cr = new ClassReader(getClass().getName());
    CheckClassAdapter.verify(cr, true, new PrintWriter(new StringWriter()));
  }

  @Test
  public void testVerifyInvalidClass() {
    ClassWriter cw = new ClassWriter(0);
    cw.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "m", "()V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ISTORE, 30);
    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 31);
    mv.visitEnd();
    cw.visitEnd();
    ClassReader cr = new ClassReader(cw.toByteArray());
    CheckClassAdapter.verify(cr, true, new PrintWriter(new StringWriter()));
  }

  @Test
  public void testIllegalClassAccessFlag() {
    ClassVisitor cv = new CheckClassAdapter(null);
    assertThrows(
        Exception.class, () -> cv.visit(V1_1, 1 << 20, "C", null, "java/lang/Object", null));
  }

  @Test
  public void testIllegalSuperClass() {
    ClassVisitor cv = new CheckClassAdapter(null);
    assertThrows(
        Exception.class,
        () -> cv.visit(V1_1, ACC_PUBLIC, "java/lang/Object", null, "java/lang/Object", null));
  }

  @Test
  public void testIllegalInterfaceSuperClass() {
    ClassVisitor cv = new CheckClassAdapter(null);
    assertThrows(Exception.class, () -> cv.visit(V1_1, ACC_INTERFACE, "I", null, "C", null));
  }

  @Test
  public void testIllegalClassSignature() {
    ClassVisitor cv = new CheckClassAdapter(null);
    assertThrows(
        Exception.class, () -> cv.visit(V1_1, ACC_PUBLIC, "C", "LC;I", "java/lang/Object", null));
  }

  @Test
  public void testIllegalClassAccessFlagSet() {
    ClassVisitor cv = new CheckClassAdapter(null);
    assertThrows(
        Exception.class,
        () -> cv.visit(V1_1, ACC_FINAL + ACC_ABSTRACT, "C", null, "java/lang/Object", null));
  }

  @Test
  public void testIllegalClassMemberVisitBeforeStart() {
    ClassVisitor cv = new CheckClassAdapter(null);
    assertThrows(Exception.class, () -> cv.visitSource(null, null));
  }

  @Test
  public void testIllegalClassAttribute() {
    ClassVisitor cv = new CheckClassAdapter(null);
    cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    assertThrows(Exception.class, () -> cv.visitAttribute(null));
  }

  @Test
  public void testIllegalMultipleVisitCalls() {
    ClassVisitor cv = new CheckClassAdapter(null);
    cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    assertThrows(
        Exception.class, () -> cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null));
  }

  @Test
  public void testIllegalMultipleVisitSourceCalls() {
    ClassVisitor cv = new CheckClassAdapter(null);
    cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    cv.visitSource(null, null);
    assertThrows(Exception.class, () -> cv.visitSource(null, null));
  }

  @Test
  public void testIllegalOuterClassName() {
    ClassVisitor cv = new CheckClassAdapter(null);
    cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    assertThrows(Exception.class, () -> cv.visitOuterClass(null, null, null));
  }

  @Test
  public void testIllegalMultipleVisitOuterClassCalls() {
    ClassVisitor cv = new CheckClassAdapter(null);
    cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    cv.visitOuterClass("name", null, null);
    assertThrows(Exception.class, () -> cv.visitOuterClass(null, null, null));
  }

  @Test
  public void testIllegalFieldAccessFlagSet() {
    ClassVisitor cv = new CheckClassAdapter(null);
    cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    assertThrows(
        Exception.class, () -> cv.visitField(ACC_PUBLIC + ACC_PRIVATE, "i", "I", null, null));
  }

  @Test
  public void testIllegalFieldSignature() {
    ClassVisitor cv = new CheckClassAdapter(null);
    cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    assertThrows(Exception.class, () -> cv.visitField(ACC_PUBLIC, "i", "I", "L;", null));
    assertThrows(Exception.class, () -> cv.visitField(ACC_PUBLIC, "i", "I", "LC+", null));
    assertThrows(Exception.class, () -> cv.visitField(ACC_PUBLIC, "i", "I", "LC;I", null));
  }

  @Test
  public void testIllegalClassMemberVisitAfterEnd() {
    ClassVisitor cv = new CheckClassAdapter(null);
    cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    cv.visitEnd();
    assertThrows(Exception.class, () -> cv.visitSource(null, null));
  }

  @Test
  public void testIllegalFieldMemberVisitAfterEnd() {
    FieldVisitor fv = new CheckFieldAdapter(null);
    fv.visitEnd();
    assertThrows(Exception.class, () -> fv.visitAttribute(new Comment()));
  }

  @Test
  public void testIllegalFieldAttribute() {
    FieldVisitor fv = new CheckFieldAdapter(null);
    assertThrows(Exception.class, () -> fv.visitAttribute(null));
  }

  @Test
  public void testIllegalAnnotationDesc() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    assertThrows(Exception.class, () -> mv.visitParameterAnnotation(0, "'", true));
  }

  @Test
  public void testIllegalAnnotationName() {
    AnnotationVisitor av = new CheckAnnotationAdapter(null);
    assertThrows(Exception.class, () -> av.visit(null, new Integer(0)));
  }

  @Test
  public void testIllegalAnnotationValue() {
    AnnotationVisitor av = new CheckAnnotationAdapter(null);
    assertThrows(Exception.class, () -> av.visit("name", new Object()));
  }

  @Test
  public void testIllegalAnnotationEnumValue() {
    AnnotationVisitor av = new CheckAnnotationAdapter(null);
    assertThrows(Exception.class, () -> av.visitEnum("name", "Lpkg/Enum;", null));
  }

  @Test
  public void testIllegalAnnotationValueAfterEnd() {
    AnnotationVisitor av = new CheckAnnotationAdapter(null);
    av.visitEnd();
    assertThrows(Exception.class, () -> av.visit("name", new Integer(0)));
  }

  @Test
  public void testIllegalMethodMemberVisitAfterEnd() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitEnd();
    assertThrows(Exception.class, () -> mv.visitAttribute(new Comment()));
  }

  @Test
  public void testIllegalMethodAttribute() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    assertThrows(Exception.class, () -> mv.visitAttribute(null));
  }

  @Test
  public void testIllegalMethodSignature() {
    ClassVisitor cv = new CheckClassAdapter(null);
    cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    assertThrows(
        Exception.class,
        () -> cv.visitMethod(ACC_PUBLIC, "m", "()V", "<T::LI.J<*+LA;>;>()V^LA;X", null));
  }

  @Test
  public void testIllegalMethodInsnVisitBeforeStart() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    assertThrows(Exception.class, () -> mv.visitInsn(NOP));
  }

  @Test
  public void testIllegalFrameType() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFrame(123, 0, null, 0, null));
  }

  @Test
  public void testIllegalFrameLocalCount() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFrame(F_SAME, 1, new Object[] {INTEGER}, 0, null));
  }

  @Test
  public void testIllegalFrameStackCount() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFrame(F_SAME, 0, null, 1, new Object[] {INTEGER}));
  }

  @Test
  public void testIllegalFrameLocalArray() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFrame(F_APPEND, 1, new Object[0], 0, null));
  }

  @Test
  public void testIllegalFrameStackArray() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFrame(F_SAME1, 0, null, 1, new Object[0]));
  }

  @Test
  public void testIllegalFrameValue() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFrame(F_FULL, 1, new Object[] {"LC;"}, 0, null));
    assertThrows(
        Exception.class, () -> mv.visitFrame(F_FULL, 1, new Object[] {new Integer(0)}, 0, null));
  }

  @Test
  public void testIllegalMethodInsn() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitInsn(-1));
  }

  @Test
  public void testIllegalByteInsnOperand() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitIntInsn(BIPUSH, Integer.MAX_VALUE));
  }

  @Test
  public void testIllegalShortInsnOperand() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitIntInsn(SIPUSH, Integer.MAX_VALUE));
  }

  @Test
  public void testIllegalVarInsnOperand() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitVarInsn(ALOAD, -1));
  }

  @Test
  public void testIllegalIntInsnOperand() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitIntInsn(NEWARRAY, 0));
  }

  @Test
  public void testIllegalTypeInsnOperand() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitTypeInsn(NEW, "[I"));
  }

  @Test
  public void testIllegalLabelInsnOperand() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    Label l = new Label();
    mv.visitLabel(l);
    assertThrows(Exception.class, () -> mv.visitLabel(l));
  }

  @Test
  public void testIllegalDebugLabelUse() throws IOException {
    ClassReader cr = new ClassReader("java.lang.Object");
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    ClassVisitor cv =
        new ClassVisitor(Opcodes.ASM5, cw) {
          @Override
          public MethodVisitor visitMethod(
              int access, String name, String desc, String signature, String[] exceptions) {
            final MethodVisitor next = cv.visitMethod(access, name, desc, signature, exceptions);
            if (next == null) {
              return next;
            }
            return new MethodVisitor(Opcodes.ASM5, new CheckMethodAdapter(next)) {
              private Label entryLabel = null;

              @Override
              public void visitLabel(Label label) {
                if (entryLabel == null) {
                  entryLabel = label;
                }
                mv.visitLabel(label);
              }

              @Override
              public void visitMaxs(int maxStack, int maxLocals) {
                Label unwindhandler = new Label();
                mv.visitLabel(unwindhandler);
                mv.visitInsn(Opcodes.ATHROW); // rethrow
                mv.visitTryCatchBlock(entryLabel, unwindhandler, unwindhandler, null);
                mv.visitMaxs(maxStack, maxLocals);
              }
            };
          }
        };
    assertThrows(Exception.class, () -> cr.accept(cv, ClassReader.EXPAND_FRAMES));
  }

  @Test
  public void testIllegalTableSwitchParameters1() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitTableSwitchInsn(1, 0, new Label(), new Label[0]));
  }

  @Test
  public void testIllegalTableSwitchParameters2() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitTableSwitchInsn(0, 1, null, new Label[0]));
  }

  @Test
  public void testIllegalTableSwitchParameters3() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitTableSwitchInsn(0, 1, new Label(), (Label[]) null));
  }

  @Test
  public void testIllegalTableSwitchParameters4() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitTableSwitchInsn(0, 1, new Label(), new Label[0]));
  }

  @Test
  public void testIllegalLookupSwitchParameters1() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitLookupSwitchInsn(new Label(), null, new Label[0]));
  }

  @Test
  public void testIllegalLookupSwitchParameters2() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitLookupSwitchInsn(new Label(), new int[0], null));
  }

  @Test
  public void testIllegalLookupSwitchParameters3() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(
        Exception.class, () -> mv.visitLookupSwitchInsn(new Label(), new int[0], new Label[1]));
  }

  @Test
  public void testIllegalFieldInsnNullOwner() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFieldInsn(GETFIELD, null, "i", "I"));
  }

  @Test
  public void testIllegalFieldInsnOwner() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFieldInsn(GETFIELD, "-", "i", "I"));
  }

  @Test
  public void testIllegalFieldInsnNullName() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFieldInsn(GETFIELD, "C", null, "I"));
  }

  @Test
  public void testIllegalFieldInsnName() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFieldInsn(GETFIELD, "C", "-", "I"));
  }

  @Test
  public void testIllegalFieldInsnName2() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFieldInsn(GETFIELD, "C", "a-", "I"));
  }

  @Test
  public void testIllegalFieldInsnNullDesc() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFieldInsn(GETFIELD, "C", "i", null));
  }

  @Test
  public void testIllegalFieldInsnVoidDesc() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFieldInsn(GETFIELD, "C", "i", "V"));
  }

  @Test
  public void testIllegalFieldInsnPrimitiveDesc() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFieldInsn(GETFIELD, "C", "i", "II"));
  }

  @Test
  public void testIllegalFieldInsnArrayDesc() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFieldInsn(GETFIELD, "C", "i", "["));
  }

  @Test
  public void testIllegalFieldInsnReferenceDesc() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFieldInsn(GETFIELD, "C", "i", "L"));
  }

  @Test
  public void testIllegalFieldInsnReferenceDesc2() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitFieldInsn(GETFIELD, "C", "i", "L-;"));
  }

  @Test
  public void testIllegalMethodInsnNullName() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMethodInsn(INVOKEVIRTUAL, "C", null, "()V", false));
  }

  @Test
  public void testIllegalMethodInsnName() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMethodInsn(INVOKEVIRTUAL, "C", "-", "()V", false));
  }

  @Test
  public void testIllegalMethodInsnName2() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMethodInsn(INVOKEVIRTUAL, "C", "a-", "()V", false));
  }

  @Test
  public void testIllegalMethodInsnNullDesc() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMethodInsn(INVOKEVIRTUAL, "C", "m", null, false));
  }

  @Test
  public void testIllegalMethodInsnDesc() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "I", false));
  }

  @Test
  public void testIllegalMethodInsnParameterDesc() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "(V)V", false));
  }

  @Test
  public void testIllegalMethodInsnReturnDesc() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "()VV", false));
  }

  @Test
  public void testIllegalMethodInsnItf() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(
        Exception.class, () -> mv.visitMethodInsn(INVOKEINTERFACE, "C", "m", "()V", false));
  }

  @Test
  public void testIllegalMethodInsnItf2() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "()V", true));
  }

  @Test
  public void testIllegalMethodInsnItf3() {
    CheckMethodAdapter mv = new CheckMethodAdapter(null);
    mv.version = Opcodes.V1_7;
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMethodInsn(INVOKESPECIAL, "C", "m", "()V", true));
  }

  @Test
  public void testMethodInsnItf() {
    CheckMethodAdapter mv = new CheckMethodAdapter(null);
    mv.version = Opcodes.V1_8;
    mv.visitCode();
    mv.visitMethodInsn(INVOKESPECIAL, "C", "m", "()V", true);
  }

  @Test
  public void testIllegalLdcInsnOperand() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitLdcInsn(new Object()));
  }

  @Test
  public void testIllegalMultiANewArrayDesc() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMultiANewArrayInsn("I", 1));
  }

  @Test
  public void testIllegalMultiANewArrayDims() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMultiANewArrayInsn("[[I", 0));
  }

  @Test
  public void testIllegalMultiANewArrayDims2() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitMultiANewArrayInsn("[[I", 3));
  }

  @Test
  public void testIllegalTryCatchBlock() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    Label m = new Label();
    Label n = new Label();
    mv.visitLabel(m);
    assertThrows(Exception.class, () -> mv.visitTryCatchBlock(m, n, n, null));
    assertThrows(Exception.class, () -> mv.visitTryCatchBlock(n, m, n, null));
    assertThrows(Exception.class, () -> mv.visitTryCatchBlock(n, n, m, null));
  }

  @Test
  public void testIllegalDataflow() {
    MethodVisitor mv = new CheckMethodAdapter(ACC_PUBLIC, "m", "(I)V", null, new HashMap<>());
    mv.visitCode();
    mv.visitVarInsn(ILOAD, 1);
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 2);
    assertThrows(Exception.class, () -> mv.visitEnd());
  }

  @Test
  public void testIllegalDataflobjectweb() {
    MethodVisitor mv = new CheckMethodAdapter(ACC_PUBLIC, "m", "(I)I", null, new HashMap<>());
    mv.visitCode();
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 2);
    assertThrows(Exception.class, () -> mv.visitEnd());
  }

  @Test
  public void testIllegalLocalVariableLabels() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    Label m = new Label();
    Label n = new Label();
    mv.visitLabel(n);
    mv.visitInsn(NOP);
    mv.visitLabel(m);
    assertThrows(Exception.class, () -> mv.visitLocalVariable("i", "I", null, m, n, 0));
  }

  @Test
  public void testIllegalLineNumerLabel() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    assertThrows(Exception.class, () -> mv.visitLineNumber(0, new Label()));
  }

  @Test
  public void testIllegalInsnVisitAfterEnd() {
    MethodVisitor mv = new CheckMethodAdapter(null);
    mv.visitCode();
    mv.visitMaxs(0, 0);
    assertThrows(Exception.class, () -> mv.visitInsn(NOP));
  }

  /**
   * Tests that classes are unchanged with a ClassReader->CheckClassAdapter->ClassWriter transform.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testCheckClassAdapter_classUnchanged(
      PrecompiledClass classParameter, Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor classVisitor = new CheckClassAdapter(apiParameter.value(), classWriter, false);
    if (classParameter.isMoreRecentThan(apiParameter)) {
      assertThrows(RuntimeException.class, () -> classReader.accept(classVisitor, attributes(), 0));
      return;
    }
    classReader.accept(classVisitor, attributes(), 0);
    assertThatClass(classWriter.toByteArray()).isEqualTo(classFile);
  }

  /** Tests that {@link CheckClassAdapter.verify()} succeeds on all precompiled classes. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testCheckClassAdapter_verify(PrecompiledClass classParameter, Api apiParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    CheckClassAdapter.verify(classReader, /* dump = */ false, printWriter);
    printWriter.close();
    assertEquals("", stringWriter.toString());
  }

  private static Attribute[] attributes() {
    return new Attribute[] {new Comment(), new CodeComment()};
  }
}
