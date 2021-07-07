package io.github.karlatemp.jhf.core.utils;

import io.github.karlatemp.jhf.core.config.JHFConfig;
import io.github.karlatemp.jhf.core.redirect.StackReMapInfo;

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
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        StackTraceElement[] remapped = hiddenUp(stackTrace);
        if (remapped != stackTrace) {
            throwable.setStackTrace(remapped);
        }
    }

    public static StackTraceElement[] hiddenUp(StackTraceElement[] stackTrace) {
        if (stackTrace == null) return null;
        if (!JHFConfig.INSTANCE.hiddenStackTrack) return stackTrace;

        if (JHFConfig.INSTANCE.hiddenAll) {
            return hiddenStack1(stackTrace);
        } else {
            return hiddenStack0(stackTrace);
        }
    }

    private static void remap(StackTraceElement[] stackTrace) {
        for (StackTraceElement eln : stackTrace) {
            StackReMapInfo.remap(eln);
        }
    }

    public static StackTraceElement[] hiddenStack0(StackTraceElement[] stackTrace) {
        remap(stackTrace);
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
                return stackTraceElements.toArray(new StackTraceElement[0]);
            }
        }
        return stackTrace;
    }

    public static StackTraceElement[] hiddenStack1(StackTraceElement[] stackTrace) {
        remap(stackTrace);
        int ed = stackTrace.length;
        for (int i = ed - 1; i >= 0; i--) {
            if (doHidden(stackTrace[i])) {
                return Arrays.copyOfRange(stackTrace, i + 1, ed);
            }
        }
        return stackTrace;
    }

}
