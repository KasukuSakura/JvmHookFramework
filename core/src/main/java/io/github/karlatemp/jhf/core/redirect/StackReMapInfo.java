package io.github.karlatemp.jhf.core.redirect;

import io.github.karlatemp.jhf.api.utils.SneakyThrow;
import io.github.karlatemp.jhf.core.utils.StackTraceModify;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;

public class StackReMapInfo {
    public static class Inf {
        public String on, omn, sn, smn, fn;
    }

    public static final Collection<Inf> infs = new ConcurrentLinkedDeque<>();

    public static void register(String obf, String obfN, String sn, String smn) {
        Inf inf = new Inf();
        inf.on = obf;
        inf.omn = obfN;
        inf.sn = sn;
        inf.smn = smn;
        {
            int llf = sn.lastIndexOf('.');
            if (llf == -1) {
                inf.fn = llf + ".java";
            } else {
                inf.fn = sn.substring(llf + 1) + ".java";
            }
        }
        infs.add(inf);
    }

    public static void remap(StackTraceElement element) {
        Inf fd = null;
        for (Inf inf : infs) {
            if (inf.on.equals(element.getClassName())) {
                if (inf.omn.equals(element.getMethodName())) {
                    fd = inf;
                    break;
                }
            }
        }
        if (fd == null) return;
        try {
            StackTraceModify.set$declaringClass.invoke(element, fd.sn);
            StackTraceModify.set$methodName.invoke(element, fd.smn);
            StackTraceModify.set$fileName.invoke(element, fd.fn);
            // StackTraceModify.set$lineNumber.invoke(element, -1);
        } catch (Throwable throwable) {
            SneakyThrow.throw0(throwable);
        }
    }
}
