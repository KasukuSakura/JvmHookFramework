package testunit;

import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Field;

public class RunTester {
    public static void error() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Throwable {
        error();
        Class.forName("java.lang.String", false, new NCL());
        StackTraceElement traceElement = NCL.STE[NCL.STE.length - 2];
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
            Assertions.assertEquals(e.getMessage(), "PERMISSION DENIED");
        }
        try {
            forName.getDeclaredField("i").setAccessible(true);
        } catch (RuntimeException e) {
            e.printStackTrace(System.out);
            Assertions.assertEquals(e.getMessage(), "PERMISSION DENIED");
        }
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
