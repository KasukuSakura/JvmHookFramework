package io.github.karlatemp.jhf.api.markers;

import io.github.karlatemp.jhf.api.utils.ClassFinder;
import io.github.karlatemp.unsafeaccessor.ModuleAccess;
import io.github.karlatemp.unsafeaccessor.Root;
import io.github.karlatemp.unsafeaccessor.Unsafe;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

public class MarkerMirrorInitialize {
    public static String PubMagicAccessorImplJdkName; // Don't modify this value
    public static final Collection<Class<?>> ALLOCATED_CLASSES;

    private static void init() throws Throwable {
        ClassLoader cc = MarkerMirrorInitialize.class.getClassLoader();
        Field CUSTOM_C_FIND = null;
        ModuleAccess MA = Root.getModuleAccess();
        Object moduleTHIZ = MA.getModule(MarkerMirrorInitialize.class);

        try {
            CUSTOM_C_FIND = cc.getClass().getDeclaredField("CUSTOM_C_FIND");
        } catch (NoSuchFieldException ignore) {
        }
        String mai;
        String[] mappings = {
                "FieldAccessor",
                "FieldAccessorImpl",
                "ConstructorAccessor",
                "ConstructorAccessorImpl",
                "MethodAccessor",
                mai = "MethodAccessorImpl",
                "MagicAccessorImpl",
        };
        if (CUSTOM_C_FIND != null) {
            Map<String, Class<?>> mirrors = new HashMap<>();
            Root.openAccess(CUSTOM_C_FIND);
            CUSTOM_C_FIND.set(cc, (Function<String, Class<?>>) mirrors::get);

            Class<?> dgC = ClassFinder.findClass(
                    null,
                    "jdk.internal.reflect.DelegatingClassLoader",
                    "sun.reflect.DelegatingClassLoader"
            );
            MA.addExports(MA.getModule(Object.class), dgC.getPackage().getName(), moduleTHIZ);

            MethodHandles.Lookup lk = Root.getTrusted(dgC);
            ClassLoader dcc = (ClassLoader) lk.findConstructor(dgC, MethodType.methodType(void.class, ClassLoader.class)).invoke(cc);
            String pkgNameI = dgC.getPackage().getName().replace('.', '/') + '/';
            String pkgName = dgC.getPackage().getName() + ".";
            Unsafe usf = Unsafe.getUnsafe();

            for (String cx : mappings) {
                try (InputStream rs = MarkerMirrorInitialize.class.getResourceAsStream(cx + ".class")) {
                    assert rs != null;
                    ClassNode cn = new ClassNode();
                    new ClassReader(rs).accept(cn, 0);

                    Class<?> target = Class.forName(pkgName + cx);
                    ClassWriter mirror = new ClassWriter(0);
                    String pubN = pkgNameI + "Pub" + cx;
                    //noinspection StringEquality
                    if (cx == mai) {
                        PubMagicAccessorImplJdkName = pubN;
                    }
                    String realPubN = "io/github/karlatemp/jhf/reflect/jdk/" + cx;
                    if (target.isInterface()) {
                        mirror.visit(
                                Opcodes.V1_8,
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
                                pubN, null, "java/lang/Object", new String[]{pkgNameI + cx}
                        );
                        byte[] c = mirror.toByteArray();
                        Class<?> cvw = usf.defineClass(null, c, 0, c.length, dcc, null);
                        ALLOCATED_CLASSES.add(cvw);
                        MA.addExports(MA.getModule(cvw), dgC.getPackage().getName(), moduleTHIZ);
                        mirrors.put(cvw.getName(), cvw);

                        if (cn.interfaces == null) {
                            cn.interfaces = new ArrayList<>();
                        }
                        cn.interfaces.add(pubN);

                        ClassWriter ccw = new ClassWriter(0);
                        cn.accept(ccw);
                        c = ccw.toByteArray();
                        usf.defineClass(null, c, 0, c.length, cc, null);
                    } else {
                        mirror.visit(
                                Opcodes.V1_8,
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                                pubN, null, pkgNameI + cx, null
                        );
                        MethodVisitor init = mirror.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                        init.visitVarInsn(Opcodes.ALOAD, 0);
                        init.visitMethodInsn(Opcodes.INVOKESPECIAL, pkgNameI + cx, "<init>", "()V", false);
                        init.visitInsn(Opcodes.RETURN);
                        init.visitMaxs(3, 3);

                        byte[] c = mirror.toByteArray();
                        Class<?> cvw = usf.defineClass(null, c, 0, c.length, null, null);
                        mirrors.put(cvw.getName(), cvw);
                        ALLOCATED_CLASSES.add(cvw);

                        mirror = new ClassWriter(0);
                        mirror.visit(
                                Opcodes.V1_8,
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                                realPubN, null, pubN, null
                        );
                        init = mirror.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                        init.visitVarInsn(Opcodes.ALOAD, 0);
                        init.visitMethodInsn(Opcodes.INVOKESPECIAL, pubN, "<init>", "()V", false);
                        init.visitInsn(Opcodes.RETURN);
                        init.visitMaxs(3, 3);

                        c = mirror.toByteArray();
                        cvw = usf.defineClass(null, c, 0, c.length, dcc, null);
                        ALLOCATED_CLASSES.add(cvw);
                        mirrors.put(cvw.getName(), cvw);


                        MA.addExports(MA.getModule(cvw), dgC.getPackage().getName(), moduleTHIZ);
                        MA.addOpens(MA.getModule(cvw), dgC.getPackage().getName(), moduleTHIZ);
                        MA.addReads(moduleTHIZ, MA.getModule(cvw));
                        mirrors.put(cvw.getName(), cvw);

                        cn.superName = realPubN;
                        for (MethodNode m : cn.methods) {
                            if (m.name.equals("<init>")) {
                                for (AbstractInsnNode ain : m.instructions) {
                                    if (ain instanceof MethodInsnNode) {
                                        MethodInsnNode min = (MethodInsnNode) ain;
                                        if (min.getOpcode() == Opcodes.INVOKESPECIAL && min.name.equals("<init>")) {
                                            min.owner = realPubN;
                                        }
                                    }
                                }
                            }
                        }

                        ClassWriter ccw = new ClassWriter(0);
                        cn.accept(ccw);
                        c = ccw.toByteArray();
                        usf.defineClass(null, c, 0, c.length, cc, null);
                    }
                }
            }


            CUSTOM_C_FIND.set(cc, null);
        }
    }

    static {
        try {
            ALLOCATED_CLASSES = new HashSet<>();
            init();
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }

    public static void initialize() {
        // Call <clinit>
    }
}
