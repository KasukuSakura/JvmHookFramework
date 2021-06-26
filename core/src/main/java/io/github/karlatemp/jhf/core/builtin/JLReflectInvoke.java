package io.github.karlatemp.jhf.core.builtin;

import io.github.karlatemp.jhf.api.events.JavaLangReflectInvokeEvent;
import io.github.karlatemp.jhf.core.utils.ClassFinder;
import io.github.karlatemp.unsafeaccessor.Root;
import io.github.karlatemp.unsafeaccessor.Unsafe;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

import java.io.PrintStream;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

public class JLReflectInvoke implements ClassNodeProcessor {
    public static class InvokeBridge {
        public static void preInvoke(Method method, Object thiz, Object[] args, Class<?> caller) throws InvocationTargetException, IllegalAccessException {
            JavaLangReflectInvokeEvent.EVENT_LINE.post(new JavaLangReflectInvokeEvent(
                    method, thiz, caller, args
            ));
        }

        public static void preGet(Field field, Object thiz, Class<?> caller) throws IllegalAccessException {
        }

        public static void preSet(Field field, Object thiz, Class<?> caller) throws IllegalAccessException {
        }

        public static void preAllocate(Constructor<?> constructor, Object[] args, Class<?> caller) throws IllegalAccessException, InvocationTargetException {
            JavaLangReflectInvokeEvent.EVENT_LINE.post(new JavaLangReflectInvokeEvent(
                    constructor, null, caller, args
            ));
        }
    }

    private static String bgName;

    private static void genBridge(
            ClassVisitor frontEnd,
            ClassVisitor backendEnd,
            String frontEndName,
            String frontEndTypeName,
            String bridgeName,
            String frontEndMetName,
            String metName,
            String desc,
            String callerSensitive,
            String reflection,

            String proxyName,
            Type proxyReturnType
    ) {
        String frontEndMetNameAbs = frontEndMetName + "$abs";
        String descWithCaller, proxyDesc, proxyInvokeDesc, proxyTypeName;
        Type[] argTypes = Type.getArgumentTypes(desc);
        Type retType = Type.getReturnType(desc);
        {
            Type[] t1 = Arrays.copyOf(argTypes, argTypes.length + 1);
            t1[argTypes.length] = Type.getObjectType("java/lang/Class");
            descWithCaller = Type.getMethodDescriptor(retType, t1);
        }
        {
            proxyDesc = Type.getMethodDescriptor(proxyReturnType, argTypes);
            proxyTypeName = argTypes[0].getInternalName();
            proxyInvokeDesc = Type.getMethodDescriptor(proxyReturnType, Arrays.copyOfRange(argTypes, 1, argTypes.length));
        }

        frontEnd.visitMethod(Opcodes.ACC_PROTECTED | Opcodes.ACC_ABSTRACT, frontEndMetNameAbs, descWithCaller, null, null);
        MethodVisitor staticBridge = frontEnd.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                frontEndMetName, proxyDesc,
                null, null
        );

        int slot = 0;
        staticBridge.visitFieldInsn(Opcodes.GETSTATIC, frontEndName, "i", frontEndTypeName);
        for (Type t : argTypes) {
            staticBridge.visitVarInsn(t.getOpcode(Opcodes.ILOAD), slot);
            slot += t.getSize();
        }
        staticBridge.visitMethodInsn(Opcodes.INVOKESTATIC, reflection, "getCallerClass", "()Ljava/lang/Class;", false);
        staticBridge.visitMethodInsn(Opcodes.INVOKEVIRTUAL, frontEndName, frontEndMetNameAbs, descWithCaller, false);
        switch (retType.getSize()) {
            case 0:
                break;
            case 1:
                staticBridge.visitInsn(Opcodes.POP);
                break;
            case 2:
                staticBridge.visitInsn(Opcodes.POP2);
                break;
        }

        slot = 0;
        staticBridge.visitFieldInsn(Opcodes.GETSTATIC, frontEndName, "i", frontEndTypeName);
        for (Type t : argTypes) {
            staticBridge.visitVarInsn(t.getOpcode(Opcodes.ILOAD), slot);
            slot += t.getSize();
        }
        staticBridge.visitMethodInsn(Opcodes.INVOKEVIRTUAL, proxyTypeName, proxyName, proxyInvokeDesc, false);
        slot++;

        staticBridge.visitInsn(proxyReturnType.getOpcode(Opcodes.IRETURN));
        staticBridge.visitMaxs(slot + 1, slot);

        staticBridge.visitAnnotation(callerSensitive, true);
        staticBridge.visitAnnotation(callerSensitive, false);


        slot = 1;
        MethodVisitor backendBridge = backendEnd.visitMethod(Opcodes.ACC_PROTECTED, frontEndMetNameAbs, descWithCaller, null, null);
        for (Type t : argTypes) {
            backendBridge.visitVarInsn(t.getOpcode(Opcodes.ILOAD), slot);
            slot += t.getSize();
        }
        backendBridge.visitVarInsn(Opcodes.ALOAD, slot);
        slot++;
        backendBridge.visitMethodInsn(Opcodes.INVOKESTATIC, bridgeName, metName, descWithCaller, false);
        backendBridge.visitInsn(retType.getOpcode(Opcodes.IRETURN));
        backendBridge.visitMaxs(slot, slot);
    }

    public static void init() throws Exception {
        String frontEndName = "java/lang/JLReflectInvokeRfRf$Z$$$" + UUID.randomUUID();
        bgName = frontEndName;
        String frontEndTypeName = "L" + frontEndName + ";";
        String backendName = "io/github/karlatemp/jhf/core/builtin/JLReflectInvoke$BLKB$$$" + UUID.randomUUID();
        String bridgeName = InvokeBridge.class.getName().replace('.', '/');
        // @jdk.internal.reflect.CallerSensitive
        // @sun.reflect.CallerSensitive
        String callerSensitive = "L" + ClassFinder.findClass(null,
                "jdk.internal.reflect.CallerSensitive",
                "sun.reflect.CallerSensitive"
        ).getName().replace('.', '/') + ";";
        String reflection = ClassFinder.findClass(null,
                "jdk.internal.reflect.Reflection",
                "sun.reflect.Reflection"
        ).getName().replace('.', '/');

        ClassWriter frontEndBuilder = new ClassWriter(0);
        ClassWriter backEndBuilder = new ClassWriter(0);
        frontEndBuilder.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, frontEndName, null, "java/lang/Object", null);
        backEndBuilder.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, backendName, null, frontEndName, null);

        frontEndBuilder.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "i", "L" + frontEndName + ";", null, null);

        genBridge(frontEndBuilder, backEndBuilder, frontEndName, frontEndTypeName, bridgeName,
                "invokeMethod", "preInvoke",
                MethodType.methodType(void.class, Method.class, Object.class, Object[].class)
                        .toMethodDescriptorString(),
                callerSensitive, reflection,
                "invoke", Type.getType(Object.class)
        );
        genBridge(frontEndBuilder, backEndBuilder, frontEndName, frontEndTypeName, bridgeName,
                "invokeConstructor", "preAllocate",
                MethodType.methodType(void.class, Constructor.class, Object[].class)
                        .toMethodDescriptorString(),
                callerSensitive, reflection,
                "newInstance", Type.getType(Object.class)
        );
        byte[] frontEndCode = frontEndBuilder.toByteArray();
        byte[] backEndCode = backEndBuilder.toByteArray();
        Unsafe usf = Unsafe.getUnsafe();
        Class<?> ft = usf.defineClass(null, frontEndCode, 0, frontEndCode.length, null, null);
        Class<?> bk = usf.defineAnonymousClass(ReflectionLimit.JLBridge.class, backEndCode, null);
        {
            Field ins = ft.getDeclaredField("i");
            Root.openAccess(ins).set(null, usf.allocateInstance(bk));
        }
    }

    public static void main(String[] args) throws Exception {
        init();
        Class<?> bgc = Class.forName(bgName.replace('/', '.'));
        System.out.println(bgc);
        for (Method mt : bgc.getDeclaredMethods()) {
            System.out.println(mt);
        }
        Method mt = bgc.getMethod("invokeMethod", Method.class, Object.class, Object[].class);
        Method mtw = PrintStream.class.getMethod("println", String.class);
        mt.invoke(null, mtw, System.out, new Object[]{"This method is invoked!"});
    }

    @Override
    public ClassNode transform(ClassNode node) {
        return null;
    }
}
