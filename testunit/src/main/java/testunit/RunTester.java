package testunit;

import org.junit.jupiter.api.Assertions;

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
            Thread.sleep(1000);
            AccessibleObject.class.getDeclaredMethod(
                    "setAccessible", boolean.class
            ).invoke(forName.getDeclaredField("i"), true);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
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
}
