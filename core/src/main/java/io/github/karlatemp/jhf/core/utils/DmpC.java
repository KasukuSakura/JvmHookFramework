package io.github.karlatemp.jhf.core.utils;

import io.github.karlatemp.jhf.api.markers.MarkerMirrorInitialize;
import io.github.karlatemp.jhf.core.config.JHFConfig;
import io.github.karlatemp.mxlib.MxLib;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DmpC {
    public static final Collection<Class<?>> GENERATED_CLASSES = new ConcurrentLinkedDeque<>();
    public static final File dmpStore = new File(
            JHFConfig.workingDir, "classes-dump/" + UUID.randomUUID()
    );

    static {
        GENERATED_CLASSES.addAll(MarkerMirrorInitialize.ALLOCATED_CLASSES);
    }

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
        Class<?> resp = UAAccessHolder.UNSAFE.defineClass(null, c, 0, c.length, cl, null);
        GENERATED_CLASSES.add(resp);
        return resp;
    }

    public static Class<?> defineAnonymous(Class<?> host, byte[] c) {
        dumpToLocal(c);
        Class<?> resp = UAAccessHolder.UNSAFE.defineAnonymousClass(host, c, null);
        GENERATED_CLASSES.add(resp);
        return resp;
    }

    public static byte[] toByteCode(ClassNode node) {
        ClassWriter cw = new ClassWriter(0);
        node.accept(cw);
        return cw.toByteArray();
    }
}
