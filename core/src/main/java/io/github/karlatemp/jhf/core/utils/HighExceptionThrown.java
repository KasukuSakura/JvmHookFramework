package io.github.karlatemp.jhf.core.utils;

import io.github.karlatemp.jhf.api.utils.ClassFinder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class HighExceptionThrown {
    public static final MethodHandle newJLRInaccessibleObjectException;
    public static final MethodHandle newJLIllegalAccessException;

    static {
        try {
            MethodHandles.Lookup lk = MethodHandles.lookup();

            newJLIllegalAccessException = lk.findConstructor(
                    IllegalAccessException.class,
                    MethodType.methodType(void.class, String.class)
            );

            newJLRInaccessibleObjectException = lk.findConstructor(
                    ClassFinder.findClass(
                            null,
                            "java.lang.reflect.InaccessibleObjectException",
                            "java.lang.RuntimeException"
                    ),
                    MethodType.methodType(void.class, String.class)
            );
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }
}
