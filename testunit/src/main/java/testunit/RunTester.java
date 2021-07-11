package testunit;

import org.junit.jupiter.api.Assertions;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.opentest4j.AssertionFailedError;

import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class RunTester {
    public static void error() {
        throw new AssertionError();
    }

    public static void pubObjCtr(ClassVisitor cv) {
        MethodVisitor init = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(3, 1);
    }

    public static void main(String[] args) throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        error();

        Class.forName(RunTester.class.getName());

        Class.forName("java.lang.String", false, new NCL());
        StackTraceElement traceElement = findS(NCL.STE);
        System.out.println(traceElement);
        Class<?> forName = Class.forName(traceElement.getClassName());
        System.out.println(forName);
        for (Field f : forName.getDeclaredFields()) {
            System.out.println(f);
        }
        try {
            forName.getDeclaredField("i").get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace(System.out);
            Assertions.assertTrue(e.getMessage().contains("PERMISSION DENIED"));
        }
        try {
            forName.getDeclaredField("i").setAccessible(true);
        } catch (RuntimeException e) {
            e.printStackTrace(System.out);
            Assertions.assertTrue(e.getMessage().contains("PERMISSION DENIED"));
        }
        try {
            MethodHandles.lookup().unreflectGetter(forName.getDeclaredField("i"));
        } catch (IllegalAccessException e) {
            e.printStackTrace(System.out);
            Assertions.assertTrue(e.getMessage().contains("PERMISSION DENIED"));
        }
        try {
            Thread.sleep(1000);
            AccessibleObject.class.getDeclaredMethod(
                    "setAccessible", boolean.class
            ).invoke(forName.getDeclaredField("i"), true);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        try {
            inject(forName.getName());
            throw new AssertionFailedError("Inject class loaded.");
        } catch (LinkageError throwable) {
            throwable.printStackTrace(System.out);
        }
        try {
            ClassWriter cw = new ClassWriter(0);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "testunit/TSVC_" + counter.getAndIncrement(), null, "java/lang/Object", new String[]{"java/util/function/Consumer"});
            pubObjCtr(cw);
            MethodVisitor accept = cw.visitMethod(Opcodes.ACC_PUBLIC, "accept", "(Ljava/lang/Object;)V", null, null);
            accept.visitVarInsn(Opcodes.ALOAD, 1);
            accept.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/reflect/AccessibleObject;");
            accept.visitInsn(Opcodes.ICONST_1);
            accept.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/reflect/Method", "setAccessible", "([Ljava/lang/reflect/AccessibleObject;Z)V", false);
            accept.visitInsn(Opcodes.RETURN);
            accept.visitMaxs(5, 5);
            Consumer consumer = (Consumer) new Svc().def(cw.toByteArray()).getConstructor().newInstance();
            consumer.accept(new AccessibleObject[]{forName.getDeclaredField("i")});
        } catch (RuntimeException e) {
            e.printStackTrace(System.out);
            Assertions.assertTrue(e.getMessage().contains("PERMISSION DENIED"));
        }

        Assertions.assertNotSame(
                MethodHandles.lookup().revealDirect(
                        MethodHandles.lookup().findStatic(AccessibleObject.class, "setAccessible", MethodType.methodType(void.class, AccessibleObject[].class, boolean.class))
                ).getDeclaringClass(),
                AccessibleObject.class
        );

        RunTester.class.getDeclaredMethod("normalReflection").invoke(null);
        new FileInputStream(".jvm-hook-framework/config.conf").close();
        FileInputStream.class.getConstructor(String.class)
                .newInstance(".jvm-hook-framework/config.conf")
                .close();
    }

    private static void normalReflection() {
        System.out.println("OK");
    }

    private static void inject(String name) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, 0, "testunit/S_R_Z-USF", null, name.replace('.', '/'), null);
        Svc cl = new Svc();
        Class<?> c = cl.def(cw.toByteArray());
        System.out.println(c);
    }

    private static StackTraceElement findS(StackTraceElement[] ste) {
        StackTraceElement latest = null;
        int i = 0;
        for (; i < ste.length; i++) {
            if (ste[i].getClassName().startsWith("testunit")) {
                i++;
                break;
            }
        }
        for (; i < ste.length; i++) {
            if (!ste[i].getClassName().startsWith("java.lang.")) {
                break;
            }
            latest = ste[i];
        }
        return latest;
    }

    public static class NCL extends ClassLoader {
        static StackTraceElement[] STE;

        NCL() {
            super(null);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            STE = Thread.currentThread().getStackTrace();
            return super.loadClass(name);
        }
    }

    public static class Svc extends ClassLoader {
        public Class<?> def(byte[] code) {
            return defineClass(null, code, 0, code.length, null);
        }
    }
}
