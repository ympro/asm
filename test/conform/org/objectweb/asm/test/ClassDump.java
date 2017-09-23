/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.objectweb.asm.test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * A dump of the content of a class file, as a verbose, human "readable" String.
 * As an example, the dump of the HelloWorld class is:
 * 
 * <pre>
 * magic: -889275714
 * minor_version: 0
 * major_version: 49
 * access_flags: 33
 * this_class: ConstantClassInfo HelloWorld
 * super_class: ConstantClassInfo java/lang/Object
 * interfaces_count: 0
 * fields_count: 0
 * methods_count: 2
 * access_flags: 1
 * name_index: <init>
 * descriptor_index: ()V
 * attributes_count: 1
 * attribute_name_index: Code
 * max_stack: 1
 * max_locals: 1
 * 0: 25 0
 * 1: 183 ConstantMethodRefInfo java/lang/Object.<init>()V
 * 2: 177
 * exception_table_length: 0
 * attributes_count: 2
 * attribute_name_index: LineNumberTable
 * line_number_table_length: 1
 * start_pc: <0>
 * line_number: 31
 * attribute_name_index: LocalVariableTable
 * local_variable_table_length: 1
 * start_pc: <0>
 * length: <3>
 * name_index: this
 * descriptor_index: LHelloWorld;
 * index: 0
 * access_flags: 9
 * name_index: main
 * descriptor_index: ([Ljava/lang/String;)V
 * attributes_count: 1
 * attribute_name_index: Code
 * max_stack: 2
 * max_locals: 1
 * 0: 178 ConstantFieldRefInfo java/lang/System.outLjava/io/PrintStream;
 * 1: 18 ConstantStringInfo Hello, world!
 * 2: 182 ConstantMethodRefInfo java/io/PrintStream.println(Ljava/lang/String;)V
 * 3: 177
 * exception_table_length: 0
 * attributes_count: 2
 * attribute_name_index: LineNumberTable
 * line_number_table_length: 2
 * start_pc: <0>
 * line_number: 33
 * start_pc: <3>
 * line_number: 34
 * attribute_name_index: LocalVariableTable
 * local_variable_table_length: 1
 * start_pc: <0>
 * length: <4>
 * name_index: args
 * descriptor_index: [Ljava/lang/String;
 * index: 0
 * attributes_count: 1
 * attribute_name_index: SourceFile
 * sourcefile_index: HelloWorld.java
 * </pre>
 * 
 * This class is used to compare classes in unit tests. Its source code is as
 * close as possible to the Java Virtual Machine specification for ease of
 * reference. The constant pool and bytecode offsets are abstracted away so that
 * two classes which differ only by their constant pool or low level byte code
 * instruction representation (e.g. a ldc vs. a ldc_w) are still considered
 * equal. Likewise, attributes (resp. type annotations) are re-ordered into
 * alphabetical order, so that two classes which differ only via the ordering of
 * their attributes (resp. type annotations) are still considered equal.
 * 
 * @author Eric Bruneton
 */
class ClassDump {

    /** The dump of the input class. */
    private final String dump;

    /**
     * Creates a new ClassDump instance. The input byte array is parsed and
     * converted to a string representation by this constructor. The result can
     * then be obtained with {@link #toString}.
     * 
     * @param bytecode
     *            the content of a class file.
     * @throws IOException
     *             if class can't be parsed.
     */
    ClassDump(byte[] bytecode) throws IOException {
        Builder builder = new Builder("ClassFile", /* parent = */ null);
        dumpClassFile(new Parser(bytecode), builder);
        StringBuilder stringBuilder = new StringBuilder();
        builder.build(stringBuilder);
        this.dump = stringBuilder.toString();
    }

    @Override
    public String toString() {
        return dump;
    }

    /**
     * Parses and dumps the high level structure of the class.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.1
     */
    private static void dumpClassFile(Parser parser, Builder builder)
            throws IOException {
        builder.add("magic: ", parser.u4());
        builder.add("minor_version: ", parser.u2());
        builder.add("major_version: ", parser.u2());
        int constantPoolCount = parser.u2();
        for (int cpIndex = 1; cpIndex < constantPoolCount;) {
            CpInfo cpInfo = parseCpInfo(parser, builder);
            builder.putCpInfo(cpIndex, cpInfo);
            cpIndex += cpInfo.size();
        }
        builder.add("access_flags: ", parser.u2());
        builder.addCpInfo("this_class: ", parser.u2());
        builder.addCpInfo("super_class: ", parser.u2());
        int interfaceCount = builder.add("interfaces_count: ", parser.u2());
        for (int i = 0; i < interfaceCount; ++i) {
            builder.addCpInfo("interface: ", parser.u2());
        }
        int fieldCount = builder.add("fields_count: ", parser.u2());
        for (int i = 0; i < fieldCount; ++i) {
            dumpFieldInfo(parser, builder);
        }
        int methodCount = builder.add("methods_count: ", parser.u2());
        for (int i = 0; i < methodCount; ++i) {
            dumpMethodInfo(parser, builder);
        }
        dumpAttributeList(parser, builder);
    }

    /**
     * Parses and dumps a list of attributes.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.1
     */
    private static void dumpAttributeList(Parser parser, Builder builder)
            throws IOException {
        int attributeCount = builder.add("attributes_count: ", parser.u2());
        SortedBuilder sortedBuilder = builder.addSortedBuilder();
        for (int i = 0; i < attributeCount; ++i) {
            dumpAttributeInfo(parser, sortedBuilder);
        }
    }

    /**
     * Parses a cp_info structure.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4
     */
    private static CpInfo parseCpInfo(Parser parser, ClassContext classContext)
            throws IOException {
        int tag = parser.u1();
        switch (tag) {
        case 7:
            return new ConstantClassInfo(parser, classContext);
        case 9:
            return new ConstantFieldRefInfo(parser, classContext);
        case 10:
            return new ConstantMethodRefInfo(parser, classContext);
        case 11:
            return new ConstantInterfaceMethodRefInfo(parser, classContext);
        case 8:
            return new ConstantStringInfo(parser, classContext);
        case 3:
            return new ConstantIntegerInfo(parser);
        case 4:
            return new ConstantFloatInfo(parser);
        case 5:
            return new ConstantLongInfo(parser);
        case 6:
            return new ConstantDoubleInfo(parser);
        case 12:
            return new ConstantNameAndTypeInfo(parser, classContext);
        case 1:
            return new ConstantUtf8Info(parser);
        case 15:
            return new ConstantMethodHandleInfo(parser, classContext);
        case 16:
            return new ConstantMethodTypeInfo(parser, classContext);
        case 18:
            return new ConstantInvokeDynamicInfo(parser, classContext);
        case 19:
            return new ConstantModuleInfo(parser, classContext);
        case 20:
            return new ConstantPackageInfo(parser, classContext);
        default:
            throw new IOException("Invalid constant pool item tag " + tag);
        }
    }

    /** An abstract constant pool item. */
    private static abstract class CpInfo {
        /** The dump of this item. */
        private String dump;
        /** The context to use to get the referenced constant pool items. */
        private final ClassContext classContext;

        /** Creates a CpInfo for an item without references to other items. */
        CpInfo(String dump) {
            this.dump = dump;
            this.classContext = null;
        }

        /** Creates a CpInfo for an item with references to other items. */
        CpInfo(ClassContext classContext) {
            this.classContext = classContext;
        }

        /** Returns the number of entries used by this item in constant_pool. */
        int size() {
            return 1;
        }

        /** Returns the constant pool item with the given index. */
        <C extends CpInfo> C getCpInfo(int cpIndex, Class<C> cpInfoType) {
            return classContext.getCpInfo(cpIndex, cpInfoType);
        }

        /** Dumps this item into a string. */
        String dump() {
            return dump;
        }

        @Override
        public String toString() {
            if (dump == null) {
                dump = getClass().getSimpleName() + " " + dump();
            }
            return dump;
        }
    }

    /**
     * A CONSTANT_Class_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.1
     */
    private static class ConstantClassInfo extends CpInfo {
        private final int nameIndex;

        /** Parses a CONSTANT_Class_info item. */
        ConstantClassInfo(Parser parser, ClassContext classContext)
                throws IOException {
            super(classContext);
            this.nameIndex = parser.u2();
        }

        @Override
        String dump() {
            return getCpInfo(nameIndex, ConstantUtf8Info.class).dump();
        }
    }

    /**
     * A CONSTANT_Fieldref_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.2
     */
    private static class ConstantFieldRefInfo extends CpInfo {
        private final int classIndex;
        private final int nameAndTypeIndex;

        /** Parses a CONSTANT_Fieldref_info item. */
        ConstantFieldRefInfo(Parser parser, ClassContext classContext)
                throws IOException {
            super(classContext);
            this.classIndex = parser.u2();
            this.nameAndTypeIndex = parser.u2();
        }

        @Override
        String dump() {
            return getCpInfo(classIndex, ConstantClassInfo.class).dump() + "."
                    + getCpInfo(nameAndTypeIndex, ConstantNameAndTypeInfo.class)
                            .dump();
        }
    }

    /**
     * A CONSTANT_Methodref_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.2
     */
    private static class ConstantMethodRefInfo extends CpInfo {
        private final int classIndex;
        private final int nameAndTypeIndex;

        /** Parses a CONSTANT_Methodref_info item. */
        ConstantMethodRefInfo(Parser parser, ClassContext classContext)
                throws IOException {
            super(classContext);
            this.classIndex = parser.u2();
            this.nameAndTypeIndex = parser.u2();
        }

        @Override
        String dump() {
            return getCpInfo(classIndex, ConstantClassInfo.class).dump() + "."
                    + getCpInfo(nameAndTypeIndex, ConstantNameAndTypeInfo.class)
                            .dump();
        }
    }

    /**
     * A CONSTANT_InterfaceMethodref_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.2
     */
    private static class ConstantInterfaceMethodRefInfo extends CpInfo {
        private final int classIndex;
        private final int nameAndTypeIndex;

        /** Parses a CONSTANT_InterfaceMethodref_info item. */
        ConstantInterfaceMethodRefInfo(Parser parser, ClassContext classContext)
                throws IOException {
            super(classContext);
            this.classIndex = parser.u2();
            this.nameAndTypeIndex = parser.u2();
        }

        @Override
        String dump() {
            return getCpInfo(classIndex, ConstantClassInfo.class).dump() + "."
                    + getCpInfo(nameAndTypeIndex, ConstantNameAndTypeInfo.class)
                            .dump();
        }
    }

    /**
     * A CONSTANT_String_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.3
     */
    private static class ConstantStringInfo extends CpInfo {
        final int stringIndex;

        /** Parses a CONSTANT_String_info item. */
        ConstantStringInfo(Parser parser, ClassContext classContext)
                throws IOException {
            super(classContext);
            this.stringIndex = parser.u2();
        }

        @Override
        String dump() {
            return getCpInfo(stringIndex, ConstantUtf8Info.class).dump();
        }
    }

    /**
     * A CONSTANT_Integer_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.4
     */
    private static class ConstantIntegerInfo extends CpInfo {

        /** Parses a CONSTANT_Integer_info item. */
        ConstantIntegerInfo(Parser parser) throws IOException {
            super(Integer.toString(parser.u4()));
        }
    }

    /**
     * A CONSTANT_Float_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.4
     */
    private static class ConstantFloatInfo extends CpInfo {

        /** Parses a CONSTANT_Float_info item. */
        ConstantFloatInfo(Parser parser) throws IOException {
            super(Float.toString(Float.intBitsToFloat(parser.u4())));
        }
    }

    /**
     * A CONSTANT_Long_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.5
     */
    private static class ConstantLongInfo extends CpInfo {

        /** Parses a CONSTANT_Long_info item. */
        ConstantLongInfo(Parser parser) throws IOException {
            super(Long.toString(parser.s8()));
        }

        @Override
        int size() {
            return 2;
        }
    }

    /**
     * A CONSTANT_Double_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.5
     */
    private static class ConstantDoubleInfo extends CpInfo {

        /** Parses a CONSTANT_Double_info item. */
        ConstantDoubleInfo(Parser parser) throws IOException {
            super(Double.toString(Double.longBitsToDouble(parser.s8())));
        }

        @Override
        int size() {
            return 2;
        }
    }

    /**
     * A CONSTANT_NameAndType_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.6
     */
    private static class ConstantNameAndTypeInfo extends CpInfo {
        private final int nameIndex;
        private final int descriptorIndex;

        /** Parses a CONSTANT_NameAndType_info item. */
        ConstantNameAndTypeInfo(Parser parser, ClassContext classContext)
                throws IOException {
            super(classContext);
            this.nameIndex = parser.u2();
            this.descriptorIndex = parser.u2();
        }

        @Override
        String dump() {
            return getCpInfo(nameIndex, ConstantUtf8Info.class).dump()
                    + getCpInfo(descriptorIndex, ConstantUtf8Info.class).dump();
        }
    }

    /**
     * A CONSTANT_Utf8_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.7
     */
    private static class ConstantUtf8Info extends CpInfo {

        /** Parses a CONSTANT_Utf8_info item. */
        ConstantUtf8Info(Parser parser) throws IOException {
            super(parser.utf8());
        }
    }

    /**
     * A CONSTANT_MethodHandle_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.8
     */
    private static class ConstantMethodHandleInfo extends CpInfo {
        private final int referenceKind;
        private final int referenceIndex;

        /** Parses a CONSTANT_MethodHandle_info item. */
        ConstantMethodHandleInfo(Parser parser, ClassContext classContext)
                throws IOException {
            super(classContext);
            this.referenceKind = parser.u1();
            this.referenceIndex = parser.u2();
        }

        @Override
        String dump() {
            return referenceKind + "."
                    + getCpInfo(referenceIndex, CpInfo.class);
        }
    }

    /**
     * A CONSTANT_MethodType_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.9
     */
    private static class ConstantMethodTypeInfo extends CpInfo {
        private final int descriptorIndex;

        /** Parses a CONSTANT_MethodType_info item. */
        ConstantMethodTypeInfo(Parser parser, ClassContext classContext)
                throws IOException {
            super(classContext);
            this.descriptorIndex = parser.u2();
        }

        @Override
        String dump() {
            return getCpInfo(descriptorIndex, ConstantUtf8Info.class).dump();
        }
    }

    /**
     * A CONSTANT_InvokeDynamic_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.10
     */
    private static class ConstantInvokeDynamicInfo extends CpInfo {
        private final int bootstrapMethodAttrIndex;
        private final int nameAndTypeIndex;

        /** Parses a CONSTANT_InvokeDynamic_info item. */
        ConstantInvokeDynamicInfo(Parser parser, ClassContext classContext)
                throws IOException {
            super(classContext);
            this.bootstrapMethodAttrIndex = parser.u2();
            this.nameAndTypeIndex = parser.u2();
        }

        @Override
        String dump() {
            return bootstrapMethodAttrIndex + "."
                    + getCpInfo(nameAndTypeIndex, ConstantNameAndTypeInfo.class)
                            .dump();
        }
    }

    /**
     * A CONSTANT_Module_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.11
     */
    private static class ConstantModuleInfo extends CpInfo {
        private final int descriptorIndex;

        /** Parses a CONSTANT_Module_info item. */
        ConstantModuleInfo(Parser parser, ClassContext classContext)
                throws IOException {
            super(classContext);
            this.descriptorIndex = parser.u2();
        }

        @Override
        String dump() {
            return getCpInfo(descriptorIndex, ConstantUtf8Info.class).dump();
        }
    }

    /**
     * A CONSTANT_Package_info item.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.12
     */
    private static class ConstantPackageInfo extends CpInfo {
        private final int descriptorIndex;

        /** Parses a CONSTANT_Package_info item. */
        ConstantPackageInfo(Parser parser, ClassContext classContext)
                throws IOException {
            super(classContext);
            this.descriptorIndex = parser.u2();
        }

        @Override
        String dump() {
            return getCpInfo(descriptorIndex, ConstantUtf8Info.class).dump();
        }
    }

    /**
     * Parses and dumps a field_info structure.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.5
     */
    private static void dumpFieldInfo(Parser parser, Builder builder)
            throws IOException {
        builder.add("access_flags: ", parser.u2());
        builder.addCpInfo("name_index: ", parser.u2());
        builder.addCpInfo("descriptor_index: ", parser.u2());
        dumpAttributeList(parser, builder);
    }

    /**
     * Parses and dumps a method_info structure.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.6
     */
    private static void dumpMethodInfo(Parser parser, Builder builder)
            throws IOException {
        builder.add("access_flags: ", parser.u2());
        builder.addCpInfo("name_index: ", parser.u2());
        builder.addCpInfo("descriptor_index: ", parser.u2());
        dumpAttributeList(parser, builder);
    }

    /**
     * Parses and dumps an attribute_info structure.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7
     */
    private static void dumpAttributeInfo(Parser parser,
            SortedBuilder sortedBuilder) throws IOException {
        String attributeName = sortedBuilder.getCpInfo(parser.u2()).toString();
        int attributeLength = parser.u4();
        Builder builder = sortedBuilder.addBuilder(attributeName);
        builder.add("attribute_name_index: ", attributeName);
        if (attributeName.equals("ConstantValue")) {
            dumpConstantValueAttribute(parser, builder);
        } else if (attributeName.equals("Code")) {
            dumpCodeAttribute(parser, builder);
        } else if (attributeName.equals("CodeComment")) {
            // empty non-standard attribute used for tests.
        } else if (attributeName.equals("Comment")) {
            // empty non-standard attribute used for tests.
        } else if (attributeName.equals("StackMapTable")) {
            dumpStackMapTableAttribute(parser, builder);
        } else if (attributeName.equals("Exceptions")) {
            dumpExceptionsAttribute(parser, builder);
        } else if (attributeName.equals("InnerClasses")) {
            dumpInnerClassesAttribute(parser, builder);
        } else if (attributeName.equals("EnclosingMethod")) {
            dumpEnclosingMethodAttribute(parser, builder);
        } else if (attributeName.equals("Synthetic")) {
            dumpSyntheticAttribute(parser, builder);
        } else if (attributeName.equals("Signature")) {
            dumpSignatureAttribute(parser, builder);
        } else if (attributeName.equals("SourceFile")) {
            dumpSourceFileAttribute(parser, builder);
        } else if (attributeName.equals("SourceDebugExtension")) {
            dumpSourceDebugAttribute(attributeLength, parser, builder);
        } else if (attributeName.equals("LineNumberTable")) {
            dumpLineNumberTableAttribute(parser, builder);
        } else if (attributeName.equals("LocalVariableTable")) {
            dumpLocalVariableTableAttribute(parser, builder);
        } else if (attributeName.equals("LocalVariableTypeTable")) {
            dumpLocalVariableTypeTableAttribute(parser, builder);
        } else if (attributeName.equals("Deprecated")) {
            dumpDeprecatedAttribute(parser, builder);
        } else if (attributeName.equals("RuntimeVisibleAnnotations")) {
            dumpRuntimeVisibleAnnotationsAttribute(parser, builder);
        } else if (attributeName.equals("RuntimeInvisibleAnnotations")) {
            dumpRuntimeInvisibleAnnotationsAttribute(parser, builder);
        } else if (attributeName.equals("RuntimeVisibleParameterAnnotations")) {
            dumpRuntimeVisibleParameterAnnotationsAttribute(parser, builder);
        } else if (attributeName
                .equals("RuntimeInvisibleParameterAnnotations")) {
            dumpRuntimeInvisibleParameterAnnotationsAttribute(parser, builder);
        } else if (attributeName.equals("RuntimeVisibleTypeAnnotations")) {
            dumpRuntimeVisibleTypeAnnotationsAttribute(parser, builder);
        } else if (attributeName.equals("RuntimeInvisibleTypeAnnotations")) {
            dumpRuntimeInvisibleTypeAnnotationsAttribute(parser, builder);
        } else if (attributeName.equals("AnnotationDefault")) {
            dumpAnnotationDefaultAttribute(parser, builder);
        } else if (attributeName.equals("BootstrapMethods")) {
            dumpBootstrapMethodsAttribute(parser, builder);
        } else if (attributeName.equals("MethodParameters")) {
            dumpMethodParametersAttribute(parser, builder);
        } else if (attributeName.equals("Module")) {
            dumpModuleAttribute(parser, builder);
        } else if (attributeName.equals("ModulePackages")) {
            dumpModulePackagesAttribute(parser, builder);
        } else if (attributeName.equals("ModuleMainClass")) {
            dumpModuleMainClassAttribute(parser, builder);
        } else if (attributeName.equals("StackMap")) {
            dumpStackMapAttribute(parser, builder);
        } else {
            throw new IOException("Unknown attribute " + attributeName);
        }
    }

    /**
     * Parses and dumps a ConstantValue attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.2
     */
    private static void dumpConstantValueAttribute(Parser parser,
            Builder builder) throws IOException {
        builder.addCpInfo("constantvalue_index: ", parser.u2());
    }

    /**
     * Parses and dumps a Code attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.3
     */
    private static void dumpCodeAttribute(Parser parser, Builder builder)
            throws IOException {
        builder.add("max_stack: ", parser.u2());
        builder.add("max_locals: ", parser.u2());
        int codeLength = parser.u4();
        dumpInstructions(codeLength, parser, builder);
        int exceptionCount = builder.add("exception_table_length: ",
                parser.u2());
        for (int i = 0; i < exceptionCount; ++i) {
            builder.addInsnIndex("start_pc: ", parser.u2());
            builder.addInsnIndex("end_pc: ", parser.u2());
            builder.addInsnIndex("handler_pc: ", parser.u2());
            builder.addCpInfo("catch_type: ", parser.u2());
        }
        dumpAttributeList(parser, builder);
    }

    /**
     * The index of a bytecode instruction. This index is computed in
     * {@link #toString}, from the bytecode offset of the instruction, after the
     * whole class has been parsed. Indeed, due to forward references, the index
     * of an instruction might not be known when its offset is used.
     *
     * Dumps use instruction indices instead of bytecode offsets in order to
     * abstract away the low level byte code instruction representation details
     * (e.g. an ldc vs. an ldc_w).
     */
    private static class InstructionIndex {
        /** An offset in bytes from the start of the bytecode of a method. */
        private final int bytecodeOffset;
        /** The context to use to find the index from the bytecode offset. */
        private final MethodContext methodContext;

        InstructionIndex(int bytecodeOffset, MethodContext methodContext) {
            this.bytecodeOffset = bytecodeOffset;
            this.methodContext = methodContext;
        }

        @Override
        public String toString() {
            return "<" + methodContext.getInsnIndex(bytecodeOffset) + ">";
        }
    }

    /**
     * Parses and dumps the bytecode instructions of a method.
     * 
     * @param codeLength
     *            the number of bytes to parse.
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html#jvms-6.5
     */
    private static void dumpInstructions(int codeLength, Parser parser,
            Builder builder) throws IOException {
        int bytecodeOffset = 0; // Number of bytes parsed so far.
        int insnIndex = 0; // Number of instructions parsed so far.
        while (bytecodeOffset < codeLength) {
            builder.putInsnIndex(bytecodeOffset, insnIndex);
            int opcode = parser.u1();
            int startOffset = bytecodeOffset++;
            // Instructions are in alphabetical order of their opcode name, as
            // in the specification. This leads to some duplicated code, but is
            // done on purpose for ease of reference.
            switch (opcode) {
            case 0x32: // aaload
            case 0x53: // aastore
            case 0x01: // aconst_null
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x19: // aload
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x2A: // aload_0
            case 0x2B: // aload_1
            case 0x2C: // aload_2
            case 0x2D: // aload_3
                builder.addInsn(insnIndex, 0x19, opcode - 0x2A);
                break;
            case 0xBD: // anewarray
                builder.addInsn(insnIndex, opcode,
                        builder.getCpInfo(parser.u2()));
                bytecodeOffset += 2;
                break;
            case 0xB0: // areturn
            case 0xBE: // arraylength
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x3A: // astore
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x4B: // astore_0
            case 0x4C: // astore_1
            case 0x4D: // astore_2
            case 0x4E: // astore_3
                builder.addInsn(insnIndex, 0x3A, opcode - 0x4B);
                break;
            case 0xBF: // athrow
            case 0x33: // baload
            case 0x54: // bastore
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x10: // bipush
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x34: // caload
            case 0x55: // castore
                builder.addInsn(insnIndex, opcode);
                break;
            case 0xC0: // checkcast
                builder.addInsn(insnIndex, opcode,
                        builder.getCpInfo(parser.u2()));
                bytecodeOffset += 2;
                break;
            case 0x90: // d2f
            case 0x8E: // d2i
            case 0x8F: // d2l
            case 0x63: // dadd
            case 0x31: // daload
            case 0x52: // dastore
            case 0x98: // dcmpg
            case 0x97: // dcmpl
            case 0x0E: // dconst_0
            case 0x0F: // dconst_1
            case 0x6F: // ddiv
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x18: // dload
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x26: // dload_0
            case 0x27: // dload_1
            case 0x28: // dload_2
            case 0x29: // dload_3
                builder.addInsn(insnIndex, 0x18, opcode - 0x26);
                break;
            case 0x6B: // dmul
            case 0x77: // dneg
            case 0x73: // drem
            case 0xAF: // dreturn
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x39: // dstore
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x47: // dstore_0
            case 0x48: // dstore_1
            case 0x49: // dstore_2
            case 0x4A: // dstore_3
                builder.addInsn(insnIndex, 0x39, opcode - 0x47);
                break;
            case 0x67: // dsub
            case 0x59: // dup
            case 0x5A: // dup_x1
            case 0x5B: // dup_x2
            case 0x5C: // dup2
            case 0x5D: // dup2_x1
            case 0x5E: // dup2_x2
            case 0x8D: // f2d
            case 0x8B: // f2i
            case 0x8C: // f2l
            case 0x62: // fadd
            case 0x30: // faload
            case 0x51: // fastore
            case 0x96: // fcmpg
            case 0x95: // fcmpl
            case 0x0B: // fconst_0
            case 0x0C: // fconst_1
            case 0x0D: // fconst_2
            case 0x6E: // fdiv
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x17: // fload
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x22: // fload_0
            case 0x23: // fload_1
            case 0x24: // fload_2
            case 0x25: // fload_3
                builder.addInsn(insnIndex, 0x17, opcode - 0x22);
                break;
            case 0x6A: // fmul
            case 0x76: // fneg
            case 0x72: // frem
            case 0xAE: // freturn
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x38: // fstore
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x43: // fstore_0
            case 0x44: // fstore_1
            case 0x45: // fstore_2
            case 0x46: // fstore_3
                builder.addInsn(insnIndex, 0x38, opcode - 0x43);
                break;
            case 0x66: // fsub
                builder.addInsn(insnIndex, opcode);
                break;
            case 0xB4: // getfield
            case 0xB2: // getstatic
                builder.addInsn(insnIndex, opcode,
                        builder.getCpInfo(parser.u2()));
                bytecodeOffset += 2;
                break;
            case 0xA7: // goto
                builder.addInsn(insnIndex, opcode, new InstructionIndex(
                        startOffset + parser.s2(), builder));
                bytecodeOffset += 2;
                break;
            case 0xC8: // goto_w
                builder.addInsn(insnIndex, 0xA7, new InstructionIndex(
                        startOffset + parser.u4(), builder));
                bytecodeOffset += 4;
                break;
            case 0x91: // i2b
            case 0x92: // i2c
            case 0x87: // i2d
            case 0x86: // i2f
            case 0x85: // i2l
            case 0x93: // i2s
            case 0x60: // iadd
            case 0x2E: // iaload
            case 0x7E: // iand
            case 0x4F: // iastore
            case 0x02: // iconst_m1
            case 0x03: // iconst_0
            case 0x04: // iconst_1
            case 0x05: // iconst_2
            case 0x06: // iconst_3
            case 0x07: // iconst_4
            case 0x08: // iconst_5
            case 0x6C: // idiv
                builder.addInsn(insnIndex, opcode);
                break;
            case 0xA5: // if_acmpeq
            case 0xA6: // if_acmpne
            case 0x9F: // if_icmpeq
            case 0xA0: // if_icmpne
            case 0xA1: // if_icmplt
            case 0xA2: // if_icmpge
            case 0xA3: // if_icmpgt
            case 0xA4: // if_icmple
            case 0x99: // ifeq
            case 0x9A: // ifne
            case 0x9B: // iflt
            case 0x9C: // ifge
            case 0x9D: // ifgt
            case 0x9E: // ifle
            case 0xC7: // ifnonnull
            case 0xC6: // ifnull
                builder.addInsn(insnIndex, opcode, new InstructionIndex(
                        startOffset + parser.s2(), builder));
                bytecodeOffset += 2;
                break;
            case 0x84: // iinc
                builder.addInsn(insnIndex, opcode, parser.u1(), parser.s1());
                bytecodeOffset += 2;
                break;
            case 0x15: // iload
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x1A: // iload_0
            case 0x1B: // iload_1
            case 0x1C: // iload_2
            case 0x1D: // iload_3
                builder.addInsn(insnIndex, 0x15, opcode - 0x1A);
                break;
            case 0x68: // imul
            case 0x74: // ineg
                builder.addInsn(insnIndex, opcode);
                break;
            case 0xC1: // instanceof
                builder.addInsn(insnIndex, opcode,
                        builder.getCpInfo(parser.u2()));
                bytecodeOffset += 2;
                break;
            case 0xBA: // invokedynamic
                builder.addInsn(insnIndex, opcode,
                        builder.getCpInfo(parser.u2()));
                parser.u2();
                bytecodeOffset += 4;
                break;
            case 0xB9: // invokeinterface
                builder.addInsn(insnIndex, opcode,
                        builder.getCpInfo(parser.u2()), parser.u1());
                parser.u1();
                bytecodeOffset += 4;
                break;
            case 0xB7: // invokespecial
            case 0xB8: // invokestatic
            case 0xB6: // invokevirtual
                builder.addInsn(insnIndex, opcode,
                        builder.getCpInfo(parser.u2()));
                bytecodeOffset += 2;
                break;
            case 0x80: // ior
            case 0x70: // irem
            case 0xAC: // ireturn
            case 0x78: // ishl
            case 0x7A: // ishr
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x36: // istore
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x3B: // istore_0
            case 0x3C: // istore_1
            case 0x3D: // istore_2
            case 0x3E: // istore_3
                builder.addInsn(insnIndex, 0x36, opcode - 0x3B);
                break;
            case 0x64: // isub
            case 0x7C: // iushr
            case 0x82: // ixor
                builder.addInsn(insnIndex, opcode);
                break;
            case 0xA8: // jsr
                builder.addInsn(insnIndex, opcode, new InstructionIndex(
                        startOffset + parser.s2(), builder));
                bytecodeOffset += 2;
                break;
            case 0xC9: // jsr_w
                builder.addInsn(insnIndex, 0xA8, new InstructionIndex(
                        startOffset + parser.u4(), builder));
                bytecodeOffset += 4;
                break;
            case 0x8A: // l2d
            case 0x89: // l2f
            case 0x88: // l2i
            case 0x61: // ladd
            case 0x2F: // laload
            case 0x7F: // land
            case 0x50: // lastore
            case 0x94: // lcmp
            case 0x09: // lconst_0
            case 0x0A: // lconst_1
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x12: // ldc
                builder.addInsn(insnIndex, opcode,
                        builder.getCpInfo(parser.u1()));
                bytecodeOffset += 1;
                break;
            case 0x13: // ldc_w
            case 0x14: // ldc2_w
                builder.addInsn(insnIndex, 0x12,
                        builder.getCpInfo(parser.u2()));
                bytecodeOffset += 2;
                break;
            case 0x6D: // ldiv
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x16: // lload
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x1E: // lload_0
            case 0x1F: // lload_1
            case 0x20: // lload_2
            case 0x21: // lload_3
                builder.addInsn(insnIndex, 0x16, opcode - 0x1E);
                break;
            case 0x69: // lmul
            case 0x75: // lneg
                builder.addInsn(insnIndex, opcode);
                break;
            case 0xAB: // lookupswitch
                builder.addInsn(insnIndex, opcode);
                while (bytecodeOffset % 4 != 0) {
                    parser.u1();
                    bytecodeOffset++;
                }
                builder.addInsnIndex("default: ", startOffset + parser.u4());
                int pairCount = builder.add("npairs: ", parser.u4());
                bytecodeOffset += 8;
                for (int i = 0; i < pairCount; ++i) {
                    builder.addInsnIndex(parser.u4() + ": ",
                            startOffset + parser.u4());
                    bytecodeOffset += 8;
                }
                break;
            case 0x81: // lor
            case 0x71: // lrem
            case 0xAD: // lreturn
            case 0x79: // lshl
            case 0x7B: // lshr
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x37: // lstore
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x3F: // lstore_0
            case 0x40: // lstore_1
            case 0x41: // lstore_2
            case 0x42: // lstore_3
                builder.addInsn(insnIndex, 0x37, opcode - 0x3F);
                break;
            case 0x65: // lsub
            case 0x7D: // lushr
            case 0x83: // lxor
            case 0xC2: // monitorenter
            case 0xC3: // monitorexit
                builder.addInsn(insnIndex, opcode);
                break;
            case 0xC5: // multianewarray
                builder.addInsn(insnIndex, opcode,
                        builder.getCpInfo(parser.u2()), parser.u1());
                bytecodeOffset += 3;
                break;
            case 0xBB: // new
                builder.addInsn(insnIndex, opcode,
                        builder.getCpInfo(parser.u2()));
                bytecodeOffset += 2;
                break;
            case 0xBC: // newarray
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0x00: // nop
            case 0x57: // pop
            case 0x58: // pop2
                builder.addInsn(insnIndex, opcode);
                break;
            case 0xB5: // putfield
            case 0xB3: // putstatic
                builder.addInsn(insnIndex, opcode,
                        builder.getCpInfo(parser.u2()));
                bytecodeOffset += 2;
                break;
            case 0xA9: // ret
                builder.addInsn(insnIndex, opcode, parser.u1());
                bytecodeOffset += 1;
                break;
            case 0xB1: // return
            case 0x35: // saload
            case 0x56: // sastore
                builder.addInsn(insnIndex, opcode);
                break;
            case 0x11: // sipush
                builder.addInsn(insnIndex, opcode, parser.s2());
                bytecodeOffset += 2;
                break;
            case 0x5F: // swap
                builder.addInsn(insnIndex, opcode);
                break;
            case 0xAA: // tableswitch
                builder.addInsn(insnIndex, opcode);
                while (bytecodeOffset % 4 != 0) {
                    parser.u1();
                    bytecodeOffset++;
                }
                builder.addInsnIndex("default: ", startOffset + parser.u4());
                int low = builder.add("low: ", parser.u4());
                int high = builder.add("high: ", parser.u4());
                bytecodeOffset += 12;
                for (int i = low; i <= high; ++i) {
                    builder.addInsnIndex(i + ": ", startOffset + parser.u4());
                    bytecodeOffset += 4;
                }
                break;
            case 0xC4: // wide
                opcode = parser.u1();
                bytecodeOffset += 1;
                switch (opcode) {
                case 0x15: // iload
                case 0x17: // fload
                case 0x19: // aload
                case 0x16: // lload
                case 0x18: // dload
                case 0x36: // istore
                case 0x38: // fstore
                case 0x3A: // astore
                case 0x37: // lstore
                case 0x39: // dstore
                case 0xA9: // ret
                    builder.addInsn(insnIndex, opcode, parser.u2());
                    bytecodeOffset += 2;
                    break;
                case 0x84: // iinc
                    builder.addInsn(insnIndex, opcode, parser.u2(),
                            parser.s2());
                    bytecodeOffset += 4;
                    break;
                default:
                    throw new IOException("Unknown wide opcode: " + opcode);
                }
                break;
            default:
                throw new IOException("Unknown opcode: " + opcode);
            }
            insnIndex++;
        }
        builder.putInsnIndex(bytecodeOffset, insnIndex);
    }

    /**
     * Parses and dumps a StackMapTable attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.4
     */
    private static void dumpStackMapTableAttribute(Parser parser,
            Builder builder) throws IOException {
        int entryCount = builder.add("number_of_entries: ", parser.u2());
        int bytecodeOffset = -1;
        for (int i = 0; i < entryCount; ++i) {
            int frameType = parser.u1();
            if (frameType < 64) {
                int offsetDelta = frameType;
                bytecodeOffset += offsetDelta + 1;
                builder.addInsnIndex("SAME ", bytecodeOffset);
            } else if (frameType < 128) {
                int offsetDelta = frameType - 64;
                bytecodeOffset += offsetDelta + 1;
                builder.addInsnIndex("SAME_LOCALS_1_STACK_ITEM ",
                        bytecodeOffset);
                dumpVerificationTypeInfo(parser, builder);
            } else if (frameType < 247) {
                throw new IOException("Unknown frame type " + frameType);
            } else if (frameType == 247) {
                int offsetDelta = parser.u2();
                bytecodeOffset += offsetDelta + 1;
                builder.addInsnIndex("SAME_LOCALS_1_STACK_ITEM ",
                        bytecodeOffset);
                dumpVerificationTypeInfo(parser, builder);
            } else if (frameType < 251) {
                int offsetDelta = parser.u2();
                bytecodeOffset += offsetDelta + 1;
                builder.addInsnIndex("CHOP_" + (251 - frameType) + " ",
                        bytecodeOffset);
            } else if (frameType == 251) {
                int offsetDelta = parser.u2();
                bytecodeOffset += offsetDelta + 1;
                builder.addInsnIndex("SAME ", bytecodeOffset);
            } else if (frameType < 255) {
                int offsetDelta = parser.u2();
                bytecodeOffset += offsetDelta + 1;
                builder.addInsnIndex("APPEND_" + (frameType - 251) + " ",
                        bytecodeOffset);
                for (int j = 0; j < frameType - 251; ++j) {
                    dumpVerificationTypeInfo(parser, builder);
                }
            } else if (frameType == 255) {
                int offsetDelta = parser.u2();
                bytecodeOffset += offsetDelta + 1;
                builder.addInsnIndex("FULL ", bytecodeOffset);
                int numberOfLocals = builder.add("number_of_locals: ",
                        parser.u2());
                for (int j = 0; j < numberOfLocals; ++j) {
                    dumpVerificationTypeInfo(parser, builder);
                }
                int numberOfStackItems = builder.add("number_of_stack_items: ",
                        parser.u2());
                for (int j = 0; j < numberOfStackItems; ++j) {
                    dumpVerificationTypeInfo(parser, builder);
                }
            } else {
                throw new IOException("Unknown frame_type: " + frameType);
            }
        }
    }

    /**
     * Parses and dumps a verification_type_info structure.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.2
     */
    private static void dumpVerificationTypeInfo(Parser parser, Builder builder)
            throws IOException {
        int tag = builder.add("tag: ", parser.u1());
        if (tag > 8) {
            throw new IOException("Unknown verification_type_info tag: " + tag);
        }
        if (tag == 7) {
            builder.addCpInfo("cpool_index: ", parser.u2());
        } else if (tag == 8) {
            builder.addInsnIndex("offset: ", parser.u2());
        }
    }

    /**
     * Parses and dumps an Exception attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.5
     */
    private static void dumpExceptionsAttribute(Parser parser, Builder builder)
            throws IOException {
        int exceptionCount = builder.add("number_of_exceptions: ", parser.u2());
        for (int i = 0; i < exceptionCount; ++i) {
            builder.addCpInfo("exception_index: ", parser.u2());
        }
    }

    /**
     * Parses and dumps an InnerClasses attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.6
     */
    private static void dumpInnerClassesAttribute(Parser parser,
            Builder builder) throws IOException {
        int classCount = builder.add("number_of_classes: ", parser.u2());
        for (int i = 0; i < classCount; ++i) {
            builder.addCpInfo("inner_class_info_index: ", parser.u2());
            builder.addCpInfo("outer_class_info_index: ", parser.u2());
            builder.addCpInfo("inner_name_index: ", parser.u2());
            builder.add("inner_class_access_flags: ", parser.u2());
        }
    }

    /**
     * Parses and dumps an EnclosingMethod attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.7
     */
    private static void dumpEnclosingMethodAttribute(Parser parser,
            Builder builder) throws IOException {
        builder.addCpInfo("class_index: ", parser.u2());
        builder.addCpInfo("method_index: ", parser.u2());
    }

    /**
     * Parses and dumps a Synthetic attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.8
     */
    private static void dumpSyntheticAttribute(Parser parser, Builder builder) {
        // Nothing to parse.
    }

    /**
     * Parses and dumps a Signature attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9
     */
    private static void dumpSignatureAttribute(Parser parser, Builder builder)
            throws IOException {
        builder.addCpInfo("signature_index: ", parser.u2());
    }

    /**
     * Parses and dumps a SourceFile attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.10
     */
    private static void dumpSourceFileAttribute(Parser parser, Builder builder)
            throws IOException {
        builder.addCpInfo("sourcefile_index: ", parser.u2());
    }

    /**
     * Parses and dumps a SourceDebug attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.11
     */
    private static void dumpSourceDebugAttribute(int attributeLength,
            Parser parser, Builder builder) throws IOException {
        byte[] attributeData = parser.bytes(attributeLength);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < attributeData.length; ++i) {
            stringBuilder.append(attributeData[i]).append(',');
        }
        builder.add("debug_extension: ", stringBuilder.toString());
    }

    /**
     * Parses and dumps a LineNumberTable attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.12
     */
    private static void dumpLineNumberTableAttribute(Parser parser,
            Builder builder) throws IOException {
        int lineNumberCount = builder.add("line_number_table_length: ",
                parser.u2());
        for (int i = 0; i < lineNumberCount; ++i) {
            builder.addInsnIndex("start_pc: ", parser.u2());
            builder.add("line_number: ", parser.u2());
        }
    }

    /**
     * Parses and dumps a LocalVariableTable attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.13
     */
    private static void dumpLocalVariableTableAttribute(Parser parser,
            Builder builder) throws IOException {
        int localVariableCount = builder.add("local_variable_table_length: ",
                parser.u2());
        for (int i = 0; i < localVariableCount; ++i) {
            int startPc = builder.addInsnIndex("start_pc: ", parser.u2());
            builder.addInsnIndex("length: ", startPc + parser.u2());
            builder.addCpInfo("name_index: ", parser.u2());
            builder.addCpInfo("descriptor_index: ", parser.u2());
            builder.add("index: ", parser.u2());
        }
    }

    /**
     * Parses and dumps a LocalVariableTypeTable attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.14
     */
    private static void dumpLocalVariableTypeTableAttribute(Parser parser,
            Builder builder) throws IOException {
        int localVariableCount = builder
                .add("local_variable_type_table_length: ", parser.u2());
        for (int i = 0; i < localVariableCount; ++i) {
            int startPc = builder.addInsnIndex("start_pc: ", parser.u2());
            builder.addInsnIndex("length: ", startPc + parser.u2());
            builder.addCpInfo("name_index: ", parser.u2());
            builder.addCpInfo("signature_index: ", parser.u2());
            builder.add("index: ", parser.u2());
        }
    }

    /**
     * Parses and dumps a Deprecated attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.15
     */
    private static void dumpDeprecatedAttribute(Parser parser,
            Builder builder) {
        // Nothing to parse.
    }

    /**
     * Parses and dumps a RuntimeVisibleAnnotations attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16
     */
    private static void dumpRuntimeVisibleAnnotationsAttribute(Parser parser,
            Builder builder) throws IOException {
        int annotationCount = builder.add("num_annotations: ", parser.u2());
        for (int i = 0; i < annotationCount; ++i) {
            dumpAnnotation(parser, builder);
        }
    }

    /**
     * Parses and dumps an annotations structure.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16
     */
    private static void dumpAnnotation(Parser parser, Builder builder)
            throws IOException {
        builder.addCpInfo("type_index: ", parser.u2());
        int elementValuePairCount = builder.add("num_element_value_pairs: ",
                parser.u2());
        for (int i = 0; i < elementValuePairCount; ++i) {
            builder.addCpInfo("element_name_index: ", parser.u2());
            dumpElementValue(parser, builder);
        }
    }

    /**
     * Parses and dumps an element_value structure.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1
     */
    private static void dumpElementValue(Parser parser, Builder builder)
            throws IOException {
        int tag = parser.u1();
        switch (tag) {
        case 'B':
        case 'C':
        case 'D':
        case 'F':
        case 'I':
        case 'J':
        case 'S':
        case 'Z':
        case 's':
            builder.addCpInfo(((char) tag) + ": ", parser.u2());
            return;
        case 'e':
            builder.addCpInfo("e: ", parser.u2());
            builder.addCpInfo("const_name_index: ", parser.u2());
            return;
        case 'c':
            builder.addCpInfo(((char) tag) + ": ", parser.u2());
            return;
        case '@':
            builder.add("@: ", "");
            dumpAnnotation(parser, builder);
            return;
        case '[':
            int valueCount = builder.add("[: ", parser.u2());
            for (int i = 0; i < valueCount; ++i) {
                dumpElementValue(parser, builder);
            }
            return;
        default:
            throw new IOException("Unknown element_type tag: " + tag);
        }
    }

    /**
     * Parses and dumps a RuntimeInvisibleAnnotations attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.17
     */
    private static void dumpRuntimeInvisibleAnnotationsAttribute(Parser parser,
            Builder builder) throws IOException {
        dumpRuntimeVisibleAnnotationsAttribute(parser, builder);
    }

    /**
     * Parses and dumps a RuntimeVisibleParameterAnnotations attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.18
     */
    private static void dumpRuntimeVisibleParameterAnnotationsAttribute(
            Parser parser, Builder builder) throws IOException {
        int parameterCount = builder.add("num_parameters: ", parser.u1());
        for (int i = 0; i < parameterCount; ++i) {
            int annotationCount = builder.add("num_annotations: ", parser.u2());
            for (int j = 0; j < annotationCount; ++j) {
                dumpAnnotation(parser, builder);
            }
        }
    }

    /**
     * Parses and dumps a RuntimeInvisibleParameterAnnotations attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.19
     */
    private static void dumpRuntimeInvisibleParameterAnnotationsAttribute(
            Parser parser, Builder builder) throws IOException {
        dumpRuntimeVisibleParameterAnnotationsAttribute(parser, builder);
    }

    /**
     * Parses and dumps a RuntimeVisibleTypeAnnotations attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20
     */
    private static void dumpRuntimeVisibleTypeAnnotationsAttribute(
            Parser parser, Builder builder) throws IOException {
        int annotationCount = builder.add("num_annotations: ", parser.u2());
        SortedBuilder sortedBuilder = builder.addSortedBuilder();
        for (int i = 0; i < annotationCount; ++i) {
            dumpTypeAnnotation(parser, sortedBuilder);
        }
    }

    /**
     * Parses and dumps a type_annotation structure.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20
     */
    private static void dumpTypeAnnotation(Parser parser,
            SortedBuilder sortedBuilder) throws IOException {
        int targetType = parser.u1();
        Builder builder = sortedBuilder.addBuilder(String.valueOf(targetType));
        builder.add("target_type: ", targetType);
        switch (targetType) {
        case 0x00:
        case 0x01:
            // type_parameter_target
            builder.add("type_parameter_index: ", parser.u1());
            break;
        case 0x10:
            // supertype_target
            builder.add("supertype_index: ", parser.u2());
            break;
        case 0x11:
        case 0x12:
            // type_parameter_bound_target
            builder.add("type_parameter_index: ", parser.u1());
            builder.add("bound_index: ", parser.u1());
            break;
        case 0x13:
        case 0x14:
        case 0x15:
            // empty_target
            // Nothing to parse.
            break;
        case 0x16:
            // formal_parameter_target
            builder.add("formal_parameter_index: ", parser.u1());
            break;
        case 0x17:
            // throws_target
            builder.add("throws_type_index: ", parser.u2());
            break;
        case 0x40:
        case 0x41:
            // localvar_target
            int tableLength = builder.add("table_length: ", parser.u2());
            for (int i = 0; i < tableLength; ++i) {
                int startPc = builder.addInsnIndex("start_pc: ", parser.u2());
                builder.addInsnIndex("length: ", startPc + parser.u2());
                builder.add("index: ", parser.u2());
            }
            break;
        case 0x42:
            // catch_target
            builder.add("exception_table_index: ", parser.u2());
            break;
        case 0x43:
        case 0x44:
        case 0x45:
        case 0x46:
            // offset_target
            builder.addInsnIndex("offset: ", parser.u2());
            break;
        case 0x47:
        case 0x48:
        case 0x49:
        case 0x4A:
        case 0x4B:
            // type_argument_target
            builder.addInsnIndex("offset: ", parser.u2());
            builder.add("type_argument_index: ", parser.u1());
            break;
        default:
            throw new IOException("Unknown target_type: " + targetType);
        }
        dumpTypePath(parser, builder);
        dumpAnnotation(parser, builder);
    }

    /**
     * Parses and dumps a type_path structure.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.2
     */
    private static void dumpTypePath(Parser parser, Builder builder)
            throws IOException {
        int pathLength = builder.add("path_length: ", parser.u1());
        for (int i = 0; i < pathLength; ++i) {
            builder.add("type_path_kind: ", parser.u1());
            builder.add("type_argument_index: ", parser.u1());
        }
    }

    /**
     * Parses and dumps a RuntimeInvisibleTypeAnnotations attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.21
     */
    private static void dumpRuntimeInvisibleTypeAnnotationsAttribute(
            Parser parser, Builder builder) throws IOException {
        dumpRuntimeVisibleTypeAnnotationsAttribute(parser, builder);
    }

    /**
     * Parses and dumps an AnnotationDefault attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.22
     */
    private static void dumpAnnotationDefaultAttribute(Parser parser,
            Builder builder) throws IOException {
        dumpElementValue(parser, builder);
    }

    /**
     * Parses and dumps a BootstrapMethods attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.23
     */
    private static void dumpBootstrapMethodsAttribute(Parser parser,
            Builder builder) throws IOException {
        int bootstrapMethodCount = builder.add("num_bootstrap_methods: ",
                parser.u2());
        for (int i = 0; i < bootstrapMethodCount; ++i) {
            builder.addCpInfo("bootstrap_method_ref: ", parser.u2());
            int bootstrapArgumentCount = builder
                    .add("num_bootstrap_arguments: ", parser.u2());
            for (int j = 0; j < bootstrapArgumentCount; ++j) {
                builder.addCpInfo("bootstrap_argument: ", parser.u2());
            }
        }
    }

    /**
     * Parses and dumps a MethodParameters attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.24
     */
    private static void dumpMethodParametersAttribute(Parser parser,
            Builder builder) throws IOException {
        int parameterCount = builder.add("parameters_count: ", parser.u1());
        for (int i = 0; i < parameterCount; ++i) {
            builder.addCpInfo("name_index: ", parser.u2());
            builder.add("access_flags: ", parser.u2());
        }
    }

    /**
     * Parses and dumps a Module attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.25
     */
    private static void dumpModuleAttribute(Parser parser, Builder builder)
            throws IOException {
        builder.addCpInfo("name: ", parser.u2());
        builder.add("access: ", parser.u2());
        builder.addCpInfo("version: ", parser.u2());
        int requireCount = builder.add("require_count: ", parser.u2());
        for (int i = 0; i < requireCount; ++i) {
            builder.addCpInfo("name: ", parser.u2());
            builder.add("access: ", parser.u2());
            builder.addCpInfo("version: ", parser.u2());
        }
        int exportCount = builder.add("export_count: ", parser.u2());
        for (int i = 0; i < exportCount; ++i) {
            builder.addCpInfo("name: ", parser.u2());
            builder.add("access: ", parser.u2());
            int exportToCount = builder.add("export_to_count: ", parser.u2());
            for (int j = 0; j < exportToCount; ++j) {
                builder.addCpInfo("to: ", parser.u2());
            }
        }
        int openCount = builder.add("open_count: ", parser.u2());
        for (int i = 0; i < openCount; ++i) {
            builder.addCpInfo("name: ", parser.u2());
            builder.add("access: ", parser.u2());
            int openToCount = builder.add("open_to_count: ", parser.u2());
            for (int j = 0; j < openToCount; ++j) {
                builder.addCpInfo("to: ", parser.u2());
            }
        }
        int useCount = builder.add("use_count: ", parser.u2());
        for (int i = 0; i < useCount; ++i) {
            builder.addCpInfo("use: ", parser.u2());
        }
        int provideCount = builder.add("provide_count: ", parser.u2());
        for (int i = 0; i < provideCount; ++i) {
            builder.addCpInfo("provide: ", parser.u2());
            int provideWithCount = builder.add("provide_with_count: ",
                    parser.u2());
            for (int j = 0; j < provideWithCount; ++j) {
                builder.addCpInfo("with: ", parser.u2());
            }
        }
    }

    /**
     * Parses and dumps a ModulePackages attribute.
     * 
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.26
     */
    private static void dumpModulePackagesAttribute(Parser parser,
            Builder builder) throws IOException {
        int packageCount = builder.add("package_count: ", parser.u2());
        for (int i = 0; i < packageCount; ++i) {
            builder.addCpInfo("package: ", parser.u2());
        }
    }

    /**
     * Parses and dumps a ModuleMainClass attribute.
     *
     * @see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.27
     */
    private static void dumpModuleMainClassAttribute(Parser parser,
            Builder builder) throws IOException {
        builder.addCpInfo("main_class: ", parser.u2());
    }

    /**
     * Parses and dumps a StackMap attribute.
     *
     * @see http://docs.oracle.com/javame/config/cldc/opt-pkgs/api/cldc/api/Appendix1-verifier.pdf
     */
    private static void dumpStackMapAttribute(Parser parser, Builder builder)
            throws IOException {
        int entryCount = builder.add("number_of_entries: ", parser.u2());
        for (int i = 0; i < entryCount; ++i) {
            builder.addInsnIndex("offset: ", parser.u2());
            int numberOfLocals = builder.add("number_of_locals: ", parser.u2());
            for (int j = 0; j < numberOfLocals; ++j) {
                dumpVerificationTypeInfo(parser, builder);
            }
            int numberOfStackItems = builder.add("number_of_stack_items: ",
                    parser.u2());
            for (int j = 0; j < numberOfStackItems; ++j) {
                dumpVerificationTypeInfo(parser, builder);
            }
        }
    }

    /**
     * A simple byte array parser. The method names reflect the type names used
     * in the Java Virtual Machine Specification for ease of reference.
     */
    private static class Parser {
        private final DataInputStream dataInputStream;

        Parser(byte[] data) {
            this.dataInputStream = new DataInputStream(
                    new ByteArrayInputStream(data));
        }

        int u1() throws IOException {
            return dataInputStream.readUnsignedByte();
        }

        int s1() throws IOException {
            return dataInputStream.readByte();
        }

        int u2() throws IOException {
            return dataInputStream.readUnsignedShort();
        }

        int s2() throws IOException {
            return dataInputStream.readShort();
        }

        int u4() throws IOException {
            return dataInputStream.readInt();
        }

        long s8() throws IOException {
            long highBytes = dataInputStream.readInt();
            long lowBytes = dataInputStream.readInt() & 0xFFFFFFFFL;
            return (highBytes << 32) | lowBytes;
        }

        String utf8() throws IOException {
            return dataInputStream.readUTF();
        }

        byte[] bytes(int length) throws IOException {
            byte[] bytes = new byte[length];
            dataInputStream.readFully(bytes);
            return bytes;
        }
    }

    /** A context to lookup constant pool items from their index. */
    private static interface ClassContext {
        <C extends CpInfo> C getCpInfo(int cpIndex, Class<C> cpInfoType);
    }

    /** A context to lookup instruction indices from their bytecode offset. */
    private static interface MethodContext {
        int getInsnIndex(int bytecodeOffset);
    }

    /**
     * A helper class to build the dump of a class file. The dump can't be
     * output fully sequentially, as the input class is parsed, in particular
     * due to the re-ordering of attributes and annotations. Instead, a tree is
     * constructed first, then its nodes are sorted and finally the tree is
     * parsed in Depth First Search order to build the dump. This class is the
     * super class of the internal nodes of the tree.
     * <p>
     * Each internal node is a context that can store a mapping between constant
     * pool indices and constant pool items and between bytecode offsets and
     * instructions indices. This can be used to resolve references to such
     * objects. Contexts inherit from their parent, i.e. if a lookup fails in
     * some builder, the lookup continues in the parent, and so on until the
     * root is reached.
     */
    private static abstract class AbstractBuilder<T>
            implements ClassContext, MethodContext {
        /** Flag used to distinguish CpInfo keys in {@link #context}. */
        private final static int CP_INFO_KEY = 0xF0000000;
        /** The parent node of this node. May be null. */
        private final AbstractBuilder<?> parent;
        /** The children of this builder. */
        final ArrayList<T> children;
        /** The map used to implement the Context interfaces. */
        private final HashMap<Integer, Object> context;

        AbstractBuilder(AbstractBuilder<?> parent) {
            this.parent = parent;
            this.children = new ArrayList<T>();
            this.context = new HashMap<Integer, Object>();
        }

        /** Lookup constant pool items from their index. */
        CpInfo getCpInfo(int cpIndex) {
            return getCpInfo(cpIndex, CpInfo.class);
        }

        public <C extends CpInfo> C getCpInfo(int cpIndex,
                Class<C> cpInfoType) {
            Object cpInfo = get(CP_INFO_KEY | cpIndex);
            if (!cpInfoType.isInstance(cpInfo)) {
                throw new RuntimeException("Invalid constant pool type :"
                        + cpInfo.getClass().getName() + " should be "
                        + cpInfoType.getName());
            }
            return cpInfoType.cast(cpInfo);
        }

        public int getInsnIndex(int bytecodeOffset) {
            Integer insnIndex = (Integer) get(bytecodeOffset);
            if (insnIndex == null) {
                throw new RuntimeException(
                        "Invalid bytecode offset:" + bytecodeOffset);
            }
            return insnIndex;
        }

        /** Registers the CpInfo for the given constant pool index. */
        void putCpInfo(int cpIndex, CpInfo cpInfo) {
            context.put(CP_INFO_KEY | cpIndex, cpInfo);
        }

        /** Registers the instruction index for the given bytecode offset. */
        void putInsnIndex(int bytecodeOffset, int instructionIndex) {
            context.put(bytecodeOffset, instructionIndex);
        }

        /** Recursively appends the builder's children to the given string. */
        void build(StringBuilder stringBuilder) {
            for (Object child : children) {
                if (child instanceof AbstractBuilder<?>) {
                    ((AbstractBuilder<?>) child).build(stringBuilder);
                } else {
                    stringBuilder.append(child);
                }
            }
        }

        /**
         * Returns the value associated with the given key in this context or,
         * if not found, in the parent context (recursively).
         */
        private Object get(int key) {
            Object value = context.get(key);
            if (value != null) {
                return value;
            }
            return parent == null ? null : parent.get(key);
        }
    }

    /** An {@link AbstractBuilder} with concrete methods to add children. */
    private static class Builder extends AbstractBuilder<Object>
            implements Comparable<Builder> {
        /** The name of this builder, for sorting in {@link SortedBuilder}. */
        private final String name;

        Builder(String name, AbstractBuilder<?> parent) {
            super(parent);
            this.name = name;
        }

        /** Appends name and value to children and returns value. */
        <T> T add(String name, T value) {
            children.add(name);
            children.add(value);
            children.add("\n");
            return value;
        }

        /**
         * Appends name and the instruction index corresponding to
         * bytecodeOffset to children, and returns bytecodeOffset.
         */
        int addInsnIndex(String name, int bytecodeOffset) {
            add(name, new InstructionIndex(bytecodeOffset, this));
            return bytecodeOffset;
        }

        /** Appends the given arguments to children. */
        void addInsn(int insnIndex, int opcode, Object... arguments) {
            children.add(insnIndex);
            children.add(": ");
            children.add(opcode);
            for (Object argument : arguments) {
                children.add(" ");
                children.add(argument);
            }
            children.add("\n");
        }

        /** Appends name and the CpInfo corresponding to cpIndex to children. */
        void addCpInfo(String name, int cpIndex) {
            add(name, getCpInfo(cpIndex));
        }

        /** Appends a new {@link SortedBuilder} to children and returns it. */
        SortedBuilder addSortedBuilder() {
            SortedBuilder sortedBuilder = new SortedBuilder(this);
            children.add(sortedBuilder);
            return sortedBuilder;
        }

        public int compareTo(Builder builder) {
            return name.compareTo(builder.name);
        }
    }

    /**
     * An {@link AbstractBuilder} which sorts its children by name before
     * building.
     */
    private static class SortedBuilder extends AbstractBuilder<Builder> {
        SortedBuilder(Builder parent) {
            super(parent);
        }

        /** Appends a new {@link Builder} to children and returns it. */
        Builder addBuilder(String name) {
            Builder builder = new Builder(name, this);
            children.add(builder);
            return builder;
        }

        @Override
        void build(StringBuilder stringBuilder) {
            Collections.sort(children);
            super.build(stringBuilder);
        }
    }
}