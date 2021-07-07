package io.github.karlatemp.jhf.core.utils;

import io.github.karlatemp.jhf.core.config.JHFConfig;
import io.github.karlatemp.mxlib.MxLib;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.UUID;

public class DmpC {
    public static final File dmpStore = new File(
            JHFConfig.workingDir, "classes-dump/" + UUID.randomUUID()
    );

    public static void dumpToLocal(byte[] code) {
        if (JHFConfig.INSTANCE.saveGeneratedClasses) {
            try {
                File target = new File(
                        dmpStore, new ClassReader(code).getClassName() + ".class"
                );
                target.getParentFile().mkdirs();
                Files.write(target.toPath(), code);
            } catch (Throwable e) {
                MxLib.getLoggerOrStd("DumC").warn("Exception in dumping class", e);
            }
        }
    }

    public static void dump(byte[] bytecode) {
        new ClassReader(bytecode)
                .accept(new TraceClassVisitor(
                        null,
                        new Textifier(),
                        new PrintWriter(System.out)
                ), 0);
    }

    public static Class<?> define(ClassLoader cl, byte[] c) {
        dumpToLocal(c);
        return UAAccessHolder.UNSAFE.defineClass(null, c, 0, c.length, cl, null);
    }

    public static Class<?> defineAnonymous(Class<?> host, byte[] c) {
        dumpToLocal(c);
        return UAAccessHolder.UNSAFE.defineAnonymousClass(host, c, null);
    }
}
