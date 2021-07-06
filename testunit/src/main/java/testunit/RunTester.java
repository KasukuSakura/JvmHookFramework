package testunit;

public class RunTester {
    public static void error() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Throwable {
        error();
    }
}
