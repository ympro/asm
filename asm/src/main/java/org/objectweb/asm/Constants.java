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
package org.objectweb.asm;

/**
 * Defines additional JVM opcodes, access flags and constants which are not part of the ASM public
 * API.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html">JVMS 6</a>
 * @author Eric Bruneton
 */
interface Constants extends Opcodes {

  // ASM specific access flags.
  // WARNING: the 16 least significant bits must NOT be used, to avoid conflicts with standard
  // access flags, and also to make sure that these flags are automatically filtered out when
  // written in class files (because access flags are stored using 16 bits only).

  int ACC_CONSTRUCTOR = 0x40000; // method access flag.

  // ASM specific stack map frame types, used in {@link ClassVisitor#visitFrame}.

  /**
   * A frame inserted between already existing frames. This internal stack map frame type (in
   * addition to the ones declared in {@link Opcodes}) can only be used if the frame content can be
   * computed from the previous existing frame and from the instructions between this existing frame
   * and the inserted one, without any knowledge of the type hierarchy. This kind of frame is only
   * used when an unconditional jump is inserted in a method while expanding an ASM specific
   * instruction. Keep in sync with Opcodes.java.
   */
  int F_INSERT = 256;

  // The JVM opcode values which are not part of the ASM public API.
  // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html.

  int LDC_W = 19;
  int LDC2_W = 20;
  int ILOAD_0 = 26;
  int ILOAD_1 = 27;
  int ILOAD_2 = 28;
  int ILOAD_3 = 29;
  int LLOAD_0 = 30;
  int LLOAD_1 = 31;
  int LLOAD_2 = 32;
  int LLOAD_3 = 33;
  int FLOAD_0 = 34;
  int FLOAD_1 = 35;
  int FLOAD_2 = 36;
  int FLOAD_3 = 37;
  int DLOAD_0 = 38;
  int DLOAD_1 = 39;
  int DLOAD_2 = 40;
  int DLOAD_3 = 41;
  int ALOAD_0 = 42;
  int ALOAD_1 = 43;
  int ALOAD_2 = 44;
  int ALOAD_3 = 45;
  int ISTORE_0 = 59;
  int ISTORE_1 = 60;
  int ISTORE_2 = 61;
  int ISTORE_3 = 62;
  int LSTORE_0 = 63;
  int LSTORE_1 = 64;
  int LSTORE_2 = 65;
  int LSTORE_3 = 66;
  int FSTORE_0 = 67;
  int FSTORE_1 = 68;
  int FSTORE_2 = 69;
  int FSTORE_3 = 70;
  int DSTORE_0 = 71;
  int DSTORE_1 = 72;
  int DSTORE_2 = 73;
  int DSTORE_3 = 74;
  int ASTORE_0 = 75;
  int ASTORE_1 = 76;
  int ASTORE_2 = 77;
  int ASTORE_3 = 78;
  int WIDE = 196;
  int GOTO_W = 200;
  int JSR_W = 201;

  // Constants to convert between normal and wide jump instructions.

  // The delta between the GOTO_W and JSR_W opcodes and GOTO and JUMP.
  int WIDE_JUMP_OPCODE_DELTA = GOTO_W - GOTO;

  // Constants to convert JVM opcodes to the equivalent ASM specific opcodes, and vice versa.

  // The delta between the ASM_IFEQ, ..., ASM_IF_ACMPNE, ASM_GOTO and ASM_JSR opcodes
  // and IFEQ, ..., IF_ACMPNE, GOTO and JSR.
  int ASM_OPCODE_DELTA = 49;

  // The delta between the ASM_IFNULL and ASM_IFNONNULL opcodes and IFNULL and IFNONNULL.
  int ASM_IFNULL_OPCODE_DELTA = 20;

  // ASM specific opcodes, used for long forward jump instructions.

  int ASM_IFEQ = IFEQ + ASM_OPCODE_DELTA;
  int ASM_IFNE = IFNE + ASM_OPCODE_DELTA;
  int ASM_IFLT = IFLT + ASM_OPCODE_DELTA;
  int ASM_IFGE = IFGE + ASM_OPCODE_DELTA;
  int ASM_IFGT = IFGT + ASM_OPCODE_DELTA;
  int ASM_IFLE = IFLE + ASM_OPCODE_DELTA;
  int ASM_IF_ICMPEQ = IF_ICMPEQ + ASM_OPCODE_DELTA;
  int ASM_IF_ICMPNE = IF_ICMPNE + ASM_OPCODE_DELTA;
  int ASM_IF_ICMPLT = IF_ICMPLT + ASM_OPCODE_DELTA;
  int ASM_IF_ICMPGE = IF_ICMPGE + ASM_OPCODE_DELTA;
  int ASM_IF_ICMPGT = IF_ICMPGT + ASM_OPCODE_DELTA;
  int ASM_IF_ICMPLE = IF_ICMPLE + ASM_OPCODE_DELTA;
  int ASM_IF_ACMPEQ = IF_ACMPEQ + ASM_OPCODE_DELTA;
  int ASM_IF_ACMPNE = IF_ACMPNE + ASM_OPCODE_DELTA;
  int ASM_GOTO = GOTO + ASM_OPCODE_DELTA;
  int ASM_JSR = JSR + ASM_OPCODE_DELTA;
  int ASM_IFNULL = IFNULL + ASM_IFNULL_OPCODE_DELTA;
  int ASM_IFNONNULL = IFNONNULL + ASM_IFNULL_OPCODE_DELTA;
  int ASM_GOTO_W = 220;
}
