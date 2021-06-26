package io.github.karlatemp.jhf.core.utils;

import io.github.karlatemp.jhf.core.config.JHFConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

public class HiddenStackTrack {
    public static ArrayList<Predicate<StackTraceElement>> shouldHidden = new ArrayList<>();

    static {
        shouldHidden.add(t -> t.getClassName().startsWith("io.github.karlatemp.jhf."));
        shouldHidden.add(t -> t.getClassName().startsWith("java.lang.ReflectionLimit$B$$"));
    }

    public static void hidden(Throwable throwable) {
        if (!JHFConfig.INSTANCE.hiddenStackTrack) return;

        hiddenUp(throwable);
        hiddenUp(throwable.getCause());
        for (Throwable sup : throwable.getSuppressed()) {
            hiddenUp(sup);
        }
    }

    private static boolean doHidden(StackTraceElement stackTraceElement) {
        for (Predicate<StackTraceElement> p : shouldHidden) {
            if (p.test(stackTraceElement)) return true;
        }
        return false;
    }

    private static void hiddenUp(Throwable throwable) {
        if (throwable == null) return;
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int ed = stackTrace.length;
        for (int i = 0; i < ed; i++) {
            if (doHidden(stackTrace[i])) {
                ArrayList<StackTraceElement> stackTraceElements = new ArrayList<>(stackTrace.length);
                stackTraceElements.addAll(Arrays.asList(stackTrace).subList(0, i));
                for (int z = i + 1; z < ed; z++) {
                    if (!doHidden(stackTrace[z])) {
                        stackTraceElements.add(stackTrace[z]);
                    }
                }
                throwable.setStackTrace(stackTraceElements.toArray(new StackTraceElement[0]));
                return;
            }
        }
    }
}
