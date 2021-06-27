package io.github.karlatemp.jhf.core.utils;

import io.github.karlatemp.jhf.core.config.JHFConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

public class HiddenStackTrack {
    public static final String EVENT_PIPELINE_CLASS_NAME = "io.github.karlatemp.jhf.api.event.EventLine";
    public static ArrayList<Predicate<StackTraceElement>> shouldHidden = new ArrayList<>();

    static {
        shouldHidden.add(t -> t.getClassName().startsWith("io.github.karlatemp.jhf."));
        shouldHidden.add(t -> t.getClassName().startsWith("java.lang.ReflectionLimit$B$$"));
        shouldHidden.add(t -> t.getClassName().startsWith("java.lang.JLReflectInvokeRfRf$Z$$"));
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
        if (JHFConfig.INSTANCE.hiddenAll) {
            hidden1(throwable);
        } else {
            hidden0(throwable);
        }
    }

    private static void hidden0(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int ed = stackTrace.length;
        for (int i = ed - 1; i >= 0; i--) {
            if (doHidden(stackTrace[i])) {
                ArrayList<StackTraceElement> stackTraceElements = new ArrayList<>(stackTrace.length);
                stackTraceElements.addAll(Arrays.asList(stackTrace).subList(i + 1, ed));
                for (int z = i; z >= 0; z--) {
                    if (stackTrace[z].getClassName().equals(EVENT_PIPELINE_CLASS_NAME))
                        break;
                    if (!doHidden(stackTrace[z])) {
                        stackTraceElements.add(0, stackTrace[z]);
                    }
                }
                throwable.setStackTrace(stackTraceElements.toArray(new StackTraceElement[0]));
                return;
            }
        }
    }

    private static void hidden1(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int ed = stackTrace.length;
        for (int i = ed - 1; i >= 0; i--) {
            if (doHidden(stackTrace[i])) {
                throwable.setStackTrace(Arrays.copyOfRange(stackTrace, i + 1, ed));
                return;
            }
        }
    }
}
