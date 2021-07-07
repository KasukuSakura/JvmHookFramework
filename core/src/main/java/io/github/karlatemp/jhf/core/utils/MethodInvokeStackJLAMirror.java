package io.github.karlatemp.jhf.core.utils;

import io.github.karlatemp.unsafeaccessor.Unsafe;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static io.github.karlatemp.jhf.api.utils.RandomNameGenerator.GENERATOR;
import static io.github.karlatemp.jhf.api.utils.RandomNameGenerator.INSTANCE;

@SuppressWarnings("ConstantConditions")
public class MethodInvokeStackJLAMirror {
    interface MIS_JLA_MIRROR {
        void emit(int v);

        void emit(double v);

        void emit(short v);

        void emit(char v);

        void emit(float v);

        void emit(long v);

        void emit(boolean v);

        void emit(byte v);

        void emit(Object v);

        int poll_int();

        double poll_double();

        short poll_short();

        char poll_char();

        float poll_float();

        long poll_long();

        boolean poll_boolean();

        byte poll_byte();

        Object poll_Object();

        void release();

        void reset();

        boolean isReturned();
    }

    public static final String JLA_MIRROR_CLASS_NAME, MISIJLA_NAME, MIRROR_ALOC_NAME, MIRROR_ALLOC_DESC;
    public static final String[] EMIT_METHOD_DESC,
            POLL_METHOD_NAME,
            POLL_METHOD_DESC;

    public static final String REMAP_METHOD_M_NAME, REMAP_METHOD_M_DESC = "(Ljava/lang/reflect/Method;)Ljava/lang/reflect/Method;";

    static {
        try {
            String MISI = "io/github/karlatemp/jhf/core/utils/MethodInvokeStackImpl";
            String USF = "io/github/karlatemp/unsafeaccessor/Unsafe";
            String USFT = "L" + USF + ";";
            {
                ClassWriter mirror = new ClassWriter(0);
                try (InputStream is = MethodInvokeStackJLAMirror.class.getResourceAsStream("MethodInvokeStackJLAMirror$MIS_JLA_MIRROR.class")) {
                    ClassReader reader = new ClassReader(is);
                    ClassNode cn = new ClassNode();
                    reader.accept(cn, 0);
                    JLA_MIRROR_CLASS_NAME = cn.name = "java/lang/" + GENERATOR.getNextName(null);
                    cn.access |= Opcodes.ACC_PUBLIC;
                    cn.innerClasses.clear();
                    cn.nestHostClass = null;
                    cn.nestMembers = null;
                    cn.outerClass = null;
                    cn.outerMethod = null;
                    cn.outerMethodDesc = null;
                    cn.sourceFile = null;
                    cn.sourceDebug = null;
                    cn.accept(mirror);
                }
                byte[] mirrorBC = mirror.toByteArray();
                UAAccessHolder.UNSAFE.defineClass(null, mirrorBC, 0, mirrorBC.length, null, null);
                ClassWriter misi = new ClassWriter(0);
                misi.visit(Opcodes.V1_8, Opcodes.ACC_FINAL,
                        MISIJLA_NAME = (MISI + "$" + GENERATOR.getNextName(null)),
                        null, MISI, new String[]{JLA_MIRROR_CLASS_NAME}
                );
                for (Constructor<?> ctr : MethodInvokeStackImpl.class.getConstructors()) {
                    String desc = Type.getConstructorDescriptor(ctr);
                    MethodVisitor init = misi.visitMethod(Opcodes.ACC_PUBLIC, "<init>", desc, null, null);
                    init.visitVarInsn(Opcodes.ALOAD, 0);
                    int slot = 1;
                    for (Class<?> argT : ctr.getParameterTypes()) {
                        Type argt = Type.getType(argT);
                        init.visitVarInsn(argt.getOpcode(Opcodes.ILOAD), slot);
                        slot += argt.getSize();
                    }
                    init.visitMethodInsn(Opcodes.INVOKESPECIAL, MISI, "<init>", desc, false);
                    init.visitInsn(Opcodes.RETURN);
                    init.visitMaxs(slot, slot);
                }
                byte[] misiB = misi.toByteArray();
                UAAccessHolder.UNSAFE.defineClass(null, misiB, 0, misiB.length, MethodInvokeStackJLAMirror.class.getClassLoader(), null);
            }
            {
                MIRROR_ALOC_NAME = "java/lang/" + GENERATOR.getNextName(null);
                String MANT = "L" + MIRROR_ALOC_NAME + ";";
                ClassWriter spi = new ClassWriter(0);
                ClassWriter jlaft = new ClassWriter(0);
                jlaft.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, MIRROR_ALOC_NAME, null, "java/lang/Object", null);
                spi.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, MISI + "$$Z$" + GENERATOR.getNextName(null), null, MIRROR_ALOC_NAME, null);

                jlaft.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, "i", MANT, null, null);
                jlaft.visitMethod(Opcodes.ACC_PROTECTED | Opcodes.ACC_ABSTRACT, "alloc$abs", "(J[JIILjava/lang/Class;)Ljava/lang/Object;", null, null);

                { // hidden-stack
                    jlaft.visitMethod(Opcodes.ACC_PROTECTED | Opcodes.ACC_ABSTRACT, "hs$abs", "(Ljava/lang/Throwable;)V", null, null);
                    MethodVisitor hs = jlaft.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "hs", "(Ljava/lang/Throwable;)V", null, null);
                    hs.visitFieldInsn(Opcodes.GETSTATIC, MIRROR_ALOC_NAME, "i", MANT);
                    hs.visitVarInsn(Opcodes.ALOAD, 0);
                    hs.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MIRROR_ALOC_NAME, "hs$abs", "(Ljava/lang/Throwable;)V", false);
                    hs.visitInsn(Opcodes.RETURN);
                    hs.visitMaxs(2, 1);

                    hs = spi.visitMethod(Opcodes.ACC_PROTECTED, "hs$abs", "(Ljava/lang/Throwable;)V", null, null);
                    hs.visitVarInsn(Opcodes.ALOAD, 1);
                    hs.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/karlatemp/jhf/core/utils/HiddenStackTrack", "hidden", "(Ljava/lang/Throwable;)V", false);
                    hs.visitInsn(Opcodes.RETURN);
                    hs.visitMaxs(2, 2);
                }
                { // method-remap
                    jlaft.visitMethod(Opcodes.ACC_PROTECTED | Opcodes.ACC_ABSTRACT, "remap", REMAP_METHOD_M_DESC, null,null);
                    MethodVisitor remap = jlaft.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, REMAP_METHOD_M_NAME = "m-remap", REMAP_METHOD_M_DESC, null, null);
                    remap.visitFieldInsn(Opcodes.GETSTATIC, MIRROR_ALOC_NAME, "i", MANT);
                    remap.visitVarInsn(Opcodes.ALOAD, 0);
                    remap.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MIRROR_ALOC_NAME, "remap", REMAP_METHOD_M_DESC, false);
                    remap.visitInsn(Opcodes.ARETURN);
                    remap.visitMaxs(3, 1);

                    remap = spi.visitMethod(Opcodes.ACC_PROTECTED, "remap", REMAP_METHOD_M_DESC, null,null);
                    remap.visitVarInsn(Opcodes.ALOAD, 1);
                    remap.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/karlatemp/jhf/core/redirect/StackReMapInfo", "remap", REMAP_METHOD_M_DESC, false);
                    remap.visitInsn(Opcodes.ARETURN);
                    remap.visitMaxs(3, 2);
                }

                MethodVisitor alloc = jlaft.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "alloc", MIRROR_ALLOC_DESC = ("(J[JIILjava/lang/Class;)L" + JLA_MIRROR_CLASS_NAME + ";"), null, null);
                alloc.visitFieldInsn(Opcodes.GETSTATIC, MIRROR_ALOC_NAME, "i", MANT);
                alloc.visitVarInsn(Opcodes.LLOAD, 0);
                alloc.visitVarInsn(Opcodes.ALOAD, 2);
                alloc.visitVarInsn(Opcodes.ILOAD, 3);
                alloc.visitVarInsn(Opcodes.ILOAD, 4);
                alloc.visitVarInsn(Opcodes.ALOAD, 5);
                alloc.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MIRROR_ALOC_NAME, "alloc$abs", "(J[JIILjava/lang/Class;)Ljava/lang/Object;", false);
                alloc.visitTypeInsn(Opcodes.CHECKCAST, JLA_MIRROR_CLASS_NAME);
                alloc.visitInsn(Opcodes.ARETURN);
                alloc.visitMaxs(7, 7);

                byte[] mirror = jlaft.toByteArray();
                Class<?> b = DmpC.define(null, mirror);

                alloc = spi.visitMethod(Opcodes.ACC_PROTECTED, "alloc$abs", "(J[JIILjava/lang/Class;)Ljava/lang/Object;", null, null);
                // [this], ramsize, address, size, objects, caller, addr-base
                //      0, 1      , 3      , 4   , 5      , 6     , 7
                alloc.visitTypeInsn(Opcodes.NEW, MISIJLA_NAME);
                alloc.visitInsn(Opcodes.DUP);


                alloc.visitFieldInsn(Opcodes.GETSTATIC, "io/github/karlatemp/jhf/core/utils/UAAccessHolder", "UNSAFE", USFT);
                alloc.visitVarInsn(Opcodes.LLOAD, 1);
                alloc.visitMethodInsn(Opcodes.INVOKEVIRTUAL, USF, "allocateMemory", "(J)J", false);
                alloc.visitVarInsn(Opcodes.LSTORE, 7);


                // setMemory(long address, long bytes, byte value)
                alloc.visitFieldInsn(Opcodes.GETSTATIC, "io/github/karlatemp/jhf/core/utils/UAAccessHolder", "UNSAFE", USFT);
                alloc.visitVarInsn(Opcodes.LLOAD, 7);
                alloc.visitVarInsn(Opcodes.LLOAD, 1);
                alloc.visitInsn(Opcodes.ICONST_0);
                alloc.visitMethodInsn(Opcodes.INVOKEVIRTUAL, USF, "setMemory", "(JJB)V", false);

                alloc.visitVarInsn(Opcodes.LLOAD, 7);

                alloc.visitVarInsn(Opcodes.ALOAD, 3);
                alloc.visitVarInsn(Opcodes.ILOAD, 4);
                alloc.visitVarInsn(Opcodes.ILOAD, 5);
                alloc.visitVarInsn(Opcodes.ALOAD, 6);
                alloc.visitMethodInsn(Opcodes.INVOKESPECIAL, MISIJLA_NAME, "<init>", "(J[JIILjava/lang/Class;)V", false);
                alloc.visitInsn(Opcodes.ARETURN);
                alloc.visitMaxs(10, 9);

                byte[] impl = spi.toByteArray();
                Class<?> c = DmpC.defineAnonymous(MethodInvokeStackJLAMirror.class, impl);
                Field f = b.getDeclaredField("i");
                Unsafe unsafe = UAAccessHolder.UNSAFE;
                unsafe.putReference(
                        unsafe.staticFieldBase(f),
                        unsafe.staticFieldOffset(f),
                        unsafe.allocateInstance(c)
                );
            }
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
        {
            Type[] ts = {
                    Type.BOOLEAN_TYPE,
                    Type.BYTE_TYPE,
                    Type.SHORT_TYPE,
                    Type.CHAR_TYPE,
                    Type.INT_TYPE,
                    Type.LONG_TYPE,
                    Type.DOUBLE_TYPE,
                    Type.FLOAT_TYPE,
            };
            EMIT_METHOD_DESC = new String[15];
            POLL_METHOD_NAME = new String[15];
            POLL_METHOD_DESC = new String[15];
            for (Type t : ts) {
                EMIT_METHOD_DESC[t.getSort()] = "(" + t.getDescriptor() + ")V";
                POLL_METHOD_DESC[t.getSort()] = "()" + t.getDescriptor();
            }
            EMIT_METHOD_DESC[Type.OBJECT] = EMIT_METHOD_DESC[Type.ARRAY] = "(Ljava/lang/Object;)V";
            POLL_METHOD_DESC[Type.OBJECT] = POLL_METHOD_DESC[Type.ARRAY] = "()Ljava/lang/Object;";

            POLL_METHOD_NAME[Type.OBJECT] = POLL_METHOD_NAME[Type.ARRAY] = "poll_Object";

            POLL_METHOD_NAME[Type.BOOLEAN] = "poll_boolean";
            POLL_METHOD_NAME[Type.BYTE] = "poll_byte";
            POLL_METHOD_NAME[Type.SHORT] = "poll_short";
            POLL_METHOD_NAME[Type.CHAR] = "poll_char";
            POLL_METHOD_NAME[Type.INT] = "poll_int";
            POLL_METHOD_NAME[Type.LONG] = "poll_long";
            POLL_METHOD_NAME[Type.DOUBLE] = "poll_double";
            POLL_METHOD_NAME[Type.FLOAT] = "poll_float";
        }
    }

    public static void main(String[] args) throws Throwable {
        Class<?> jla_mirror = Class.forName(
                JLA_MIRROR_CLASS_NAME.replace('/', '.')
        );
        Class<?> misi_jla = Class.forName(
                MISIJLA_NAME.replace('/', '.')
        );
        Class<?> aloc = Class.forName(
                MIRROR_ALOC_NAME.replace('/', '.')
        );
        System.out.println(jla_mirror);
        System.out.println(misi_jla);
        System.out.println(aloc);
        Method alloc = aloc.getMethod("alloc", long.class, long[].class, int.class, int.class, Class.class);
        Object invoke = alloc.invoke(null, 30L, new long[3], 5, 2, aloc);
        System.out.println(invoke);
        jla_mirror.getMethod("release").invoke(invoke);
    }
}
