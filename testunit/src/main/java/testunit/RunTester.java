package testunit;

import org.junit.jupiter.api.Assertions;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.opentest4j.AssertionFailedError;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

public class RunTester {
    public static void error() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Throwable {
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
        RunTester.class.getDeclaredMethod("normalReflection").invoke(null);
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
