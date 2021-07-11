package io.github.karlatemp.jhf.api.utils;

import io.github.karlatemp.jhf.api.markers.MarkerMirrorInitialize;
import io.github.karlatemp.unsafeaccessor.Root;
import io.github.karlatemp.unsafeaccessor.Unsafe;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class CCL {

    interface ErrorCheck {
        void invoke() throws Throwable;
    }

    interface CCLI {
        ClassLoader cl(Class<?> c);
    }

    private static final CCLI spi;

    public static ClassLoader getClassLoader(Class<?> c) {
        return spi.cl(c);
    }

    static {
        MarkerMirrorInitialize.initialize();
        ClassWriter impl = new ClassWriter(0);
        impl.visit(
                Opcodes.V1_8, 0, "io/github/karlatemp/jhf/api/utils/CCL$CLIMPL",
                null,
                "io/github/karlatemp/jhf/api/markers/MethodAccessorImpl",
                new String[]{"io/github/karlatemp/jhf/api/utils/CCL$CCLI"}
        );

        MethodVisitor cl = impl.visitMethod(Opcodes.ACC_PUBLIC, "cl", "(Ljava/lang/Class;)Ljava/lang/ClassLoader;", null, null);
        cl.visitVarInsn(Opcodes.ALOAD, 1);

        MethodHandles.Lookup TIN = Root.getTrusted(Class.class);
        if (isNoError(() -> TIN.findGetter(Class.class, "classLoader", ClassLoader.class))) {
            cl.visitFieldInsn(Opcodes.GETFIELD, "java/lang/Class", "classLoader", "Ljava/lang/ClassLoader;");
        } else if (isNoError(() -> TIN.findVirtual(Class.class, "getClassLoader0", MethodType.methodType(ClassLoader.class)))) {
            cl.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Class", "getClassLoader0", "()Ljava/lang/ClassLoader;", false);
        } else {
            cl.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
        }
        cl.visitInsn(Opcodes.ARETURN);
        cl.visitMaxs(3, 2);

        Unsafe usf = Unsafe.getUnsafe();
        try {
            spi = (CCLI) usf.allocateInstance(usf.defineAnonymousClass(CCL.class, impl.toByteArray(), null));
        } catch (InstantiationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static boolean isNoError(ErrorCheck code) {
        try {
            code.invoke();
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }
}
