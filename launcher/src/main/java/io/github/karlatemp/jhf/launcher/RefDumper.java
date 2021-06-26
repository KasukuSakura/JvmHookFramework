package io.github.karlatemp.jhf.launcher;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

public class RefDumper {
    public static void dump(String prefix, Object value) {
        System.out.print(prefix);
        System.out.print(value);
        if (value == null) {
            System.out.println(" -{ <null> }");
            return;
        }
        if (value instanceof Enum) {
            System.out.print(" -{ ENUM <");
            System.out.print(value.getClass());
            System.out.println(">}");
            return;
        }

        String pw = prefix + "\t";
        if (value instanceof Collection<?>) {
            System.out.print(" -[ #");
            System.out.println(value.getClass());
            for (Object v : (Collection<?>) value) {
                dump(pw, v);
            }
            System.out.println("]");
            return;
        }
        System.out.print(" -{ #");
        System.out.print(value.getClass());
        if (value.getClass().getName().startsWith("java.")) {
            System.out.print(" ");
            System.out.print(value);
            System.out.println(" }");
            return;
        } else {
            System.out.println();

        }
        String ppw = pw + "\t";
        for (Field f : value.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            System.out.print(pw);
            System.out.print(f);
            System.out.println(" = ");
            f.setAccessible(true);
            try {
                dump(ppw, f.get(value));
            } catch (IllegalAccessException e) {
                System.out.print(ppw);
                System.out.print(" - { <error> ");
                //noinspection ThrowablePrintedToSystemOut
                System.out.print(e);
                System.out.println("}");
            }
        }

        System.out.print(prefix);
        System.out.println("}");

    }
}
