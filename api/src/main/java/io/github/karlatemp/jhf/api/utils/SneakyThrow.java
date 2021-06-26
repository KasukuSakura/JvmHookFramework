package io.github.karlatemp.jhf.api.utils;

@SuppressWarnings("unchecked")
public class SneakyThrow {
    public static <T2> T2 throw0(Throwable t) {
        return throw1(t);
    }

    private static <T extends Throwable, T2> T2 throw1(Throwable t) throws T {
        throw (T) t;
    }

}
