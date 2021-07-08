package io.github.karlatemp.jhf.api.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassFinder {
    public static Class<?> findClass(ClassLoader loader, String... names) throws ClassNotFoundException {
        List<Throwable> suppress = new ArrayList<>();
        for (String n : names) {
            try {
                return Class.forName(n, false, loader);
            } catch (ClassNotFoundException e) {
                Throwable cause = e.getCause();
                if (cause != null) suppress.add(cause);
                suppress.addAll(Arrays.asList(e.getSuppressed()));
            }
        }
        ClassNotFoundException cnfe = new ClassNotFoundException("Cannot found class of [" + String.join(", ", names) + "]");
        for (Throwable t : suppress) cnfe.addSuppressed(t);
        throw cnfe;
    }
}
