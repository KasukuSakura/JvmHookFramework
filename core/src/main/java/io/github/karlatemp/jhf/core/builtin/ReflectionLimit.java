package io.github.karlatemp.jhf.core.builtin;

import io.github.karlatemp.jhf.api.events.ReflectionInvokeEvent;
import io.github.karlatemp.unsafeaccessor.Root;
import io.github.karlatemp.unsafeaccessor.Unsafe;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;
import java.util.UUID;

public class ReflectionLimit implements ClassNodeProcessor {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static class JLBridge {
        private static <T> T broadcast(ReflectionInvokeEvent.Type t, T v) {
            return (T) ReflectionInvokeEvent.EVENT_LINE.post(new ReflectionInvokeEvent(t, v)).resp;
        }

        public static Method getDeclaredMethod(Method method) throws NoSuchMethodException {
            return broadcast(ReflectionInvokeEvent.Type.GetDeclaredMethod, method);
        }

        public static Method getMethod(Method method) throws NoSuchMethodException {
            return broadcast(ReflectionInvokeEvent.Type.GetMethod, method);
        }

        public static Method[] getMethods(Method[] methods) {
            return broadcast(ReflectionInvokeEvent.Type.GetMethods, methods);
        }

        public static Method[] getDeclaredMethods(Method[] methods) {
            return broadcast(ReflectionInvokeEvent.Type.GetDeclaredMethods, methods);
        }

        public static Field getField(Field field) throws NoSuchFieldException {
            return broadcast(ReflectionInvokeEvent.Type.GetField, field);
        }

        public static Field getDeclaredField(Field field) throws NoSuchFieldException {
            return broadcast(ReflectionInvokeEvent.Type.GetDeclaredField, field);
        }

        public static Field[] getFields(Field[] field) {
            return broadcast(ReflectionInvokeEvent.Type.GetFields, field);
        }

        public static Field[] getDeclaredFields(Field[] field) {
            return broadcast(ReflectionInvokeEvent.Type.GetDeclaredFields, field);
        }

        public static Constructor<?> getConstructor(Constructor<?> constructor) throws NoSuchMethodException {
            return broadcast(ReflectionInvokeEvent.Type.GetConstructor, constructor);
        }

        public static Constructor<?> getDeclaredConstructor(Constructor<?> constructor) throws NoSuchMethodException {
            return broadcast(ReflectionInvokeEvent.Type.GetDeclaredConstructor, constructor);
        }

        public static Constructor[] getConstructors(Constructor[] constructor) {
            return broadcast(ReflectionInvokeEvent.Type.GetConstructors, constructor);
        }

        public static Constructor[] getDeclaredConstructors(Constructor[] constructor) {
            return broadcast(ReflectionInvokeEvent.Type.GetDeclaredConstructors, constructor);
        }
    }

    private static String bridgeClass;
    static Collection<String> afters = new ArrayList<>();

    private static void init() {
        ClassNode frontEnd = new ClassNode();
        ClassNode backEnd = new ClassNode();

        frontEnd.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC,
                "java/lang/ReflectionLimit$B$$" + UUID.randomUUID(),
                null,
                "java/lang/Object",
                null
        );
        backEnd.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                "io/github/karlatemp/jhf/core/builtin/ReflectionLimit$B$K$Z$$" + UUID.randomUUID(),
                null,
                frontEnd.name,
                null
        );

        String instanceDesc = "L" + frontEnd.name + ";";
        frontEnd.visitField(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "i", instanceDesc, null, null
        );

        String jlbName = JLBridge.class.getName().replace('.', '/');
        for (Method method : JLBridge.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) continue;
            Type returnType = Type.getReturnType(method);
            String desc = Type.getMethodDescriptor(returnType, returnType);
            frontEnd.visitMethod(
                    Opcodes.ACC_PROTECTED | Opcodes.ACC_ABSTRACT,
                    method.getName() + "$abs",
                    desc,
                    null, null
            );
            MethodVisitor staticBridge = frontEnd.visitMethod(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    method.getName(),
                    desc,
                    null, null
            );
            staticBridge.visitFieldInsn(Opcodes.GETSTATIC, frontEnd.name, "i", instanceDesc);
            staticBridge.visitVarInsn(Opcodes.ALOAD, 0);
            staticBridge.visitMethodInsn(Opcodes.INVOKEVIRTUAL, frontEnd.name, method.getName() + "$abs", desc, false);
            staticBridge.visitInsn(Opcodes.ARETURN);
            staticBridge.visitMaxs(3, 3);

            MethodVisitor backendBridge = backEnd.visitMethod(Opcodes.ACC_PROTECTED, method.getName() + "$abs", desc, null, null);
            backendBridge.visitVarInsn(Opcodes.ALOAD, 1);
            backendBridge.visitMethodInsn(Opcodes.INVOKESTATIC, jlbName, method.getName(), desc, false);
            backendBridge.visitInsn(Opcodes.ARETURN);
            backendBridge.visitMaxs(3, 3);

            afters.add(method.getName());
        }

        ClassWriter frontEndWriter = new ClassWriter(0);
        ClassWriter backEndWriter = new ClassWriter(0);
        frontEnd.accept(frontEndWriter);
        backEnd.accept(backEndWriter);
        Unsafe usf = Unsafe.getUnsafe();
        byte[] frontEndCode = frontEndWriter.toByteArray();
        byte[] backEndCode = backEndWriter.toByteArray();
        Class<?> frontEndC = usf.defineClass(null, frontEndCode, 0, frontEndCode.length, null, null);
        Class<?> backEndC = usf.defineAnonymousClass(JLBridge.class, backEndCode, null);
        bridgeClass = frontEndC.getName().replace('.', '/');
        try {
            Field f = frontEndC.getDeclaredField("i");
            Root.openAccess(f);
            f.set(null, usf.allocateInstance(backEndC));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    ReflectionLimit() {
        init();
    }

    public static void main(String[] args) throws Throwable {
        init();
        System.out.println("Bridge: " + bridgeClass);
        Class<?> cc = Class.forName(bridgeClass.replace('/', '.'));
        Method method = cc.getMethod("getMethod", Method.class);
        method.invoke(null, method);
    }

    @Override
    public ClassNode transform(ClassNode node) {
        for (MethodNode methodNode : node.methods) {
            if (methodNode.instructions == null) continue;

            ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode ain = iterator.next();
                if (ain instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) ain;
                    if (min.owner.equals("java/lang/Class")) {
                        if (afters.contains(min.name)) {
                            Type t = Type.getReturnType(min.desc);
                            iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                    bridgeClass, min.name,
                                    Type.getMethodDescriptor(t, t),
                                    false
                            ));
                        }
                    }
                }
            }
        }
        return node;
    }
}
