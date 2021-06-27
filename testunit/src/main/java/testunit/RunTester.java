package testunit;

import org.junit.jupiter.api.Assertions;

public class RunTester {
    public static void error() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Throwable {
        error();
        try {
            EskPermissionDenied.class.getMethod("doPermissionDenied")
                    .invoke(null);
        } catch (IllegalAccessException e) {
            Assertions.assertEquals("Permission Denied", e.getMessage());
            e.printStackTrace(System.out);
        }
        try {
            EskPermissionDenied.class.getMethod("notFound");
            throw new AssertionError();
        } catch (NoSuchMethodException e) {
            e.printStackTrace(System.out);
        }

    }
}
