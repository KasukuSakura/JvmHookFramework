package io.github.karlatemp.jhf.core.redirect;

import io.github.karlatemp.jhf.api.utils.*;
import io.github.karlatemp.jhf.core.builtin.ExtendsForbidden;
import io.github.karlatemp.jhf.core.utils.DmpC;
import io.github.karlatemp.jhf.core.utils.MethodInvokeStackImpl;
import io.github.karlatemp.jhf.core.utils.MethodInvokeStackJLAMirror;
import io.github.karlatemp.jhf.core.utils.UAAccessHolder;
import io.github.karlatemp.mxlib.MxLib;
import io.github.karlatemp.unsafeaccessor.Unsafe;
import org.objectweb.asm.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

import static io.github.karlatemp.jhf.api.markers.MarkerMirrorInitialize.PubMagicAccessorImplJdkName;
import static io.github.karlatemp.jhf.api.utils.RandomNameGenerator.GENERATOR;

public class RedirectGenerator {
    private static final String

            MIS = "io/github/karlatemp/jhf/api/utils/MethodInvokeStack",
            MIST = "L" + MIS + ";",
            MTDESC = "(Ljava/lang/Object;)V",
            RMTDESC = "(" + MIST + ")V";

    public static final Collection<io.github.karlatemp.jhf.core.utils.RedirectInfos.RedirectInfo> redirectInfos = new ConcurrentLinkedDeque<>();

    private static final String callerSensitive, reflection;

    static {
        try {
            // @jdk.internal.reflect.CallerSensitive
            // @sun.reflect.CallerSensitive
            callerSensitive = "L" + ClassFinder.findClass(null,
                    "jdk.internal.reflect.CallerSensitive",
                    "sun.reflect.CallerSensitive"
            ).getName().replace('.', '/') + ";";
            reflection = ClassFinder.findClass(null,
                    "jdk.internal.reflect.Reflection",
                    "sun.reflect.Reflection"
            ).getName().replace('.', '/');
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }

    public static void generate(Class<?> mirror) {

        Iterator<Method> iterator = Stream.of(mirror.getDeclaredMethods())
                .filter(it -> Modifier.isStatic(it.getModifiers()))
                .filter(it -> it.getParameterCount() == 1)
                .filter(it -> it.getReturnType() == void.class)
                .filter(it -> it.getParameterTypes()[0] == MethodInvokeStack.class)
                .filter(it -> it.isAnnotationPresent(RedirectInfos.class))
                .iterator();

        NameGenerator metnameNG = new NonRepeatingNameGenerator(
                new HashSet<>(),
                RandomNameGenerator.INSTANCE,
                3
        );

        Collection<io.github.karlatemp.jhf.core.utils.RedirectInfos.RedirectInfo> current = new ArrayList<>();
        ClassWriter frontEndWriter = new ClassWriter(0);
        ClassWriter backEndWriter = new ClassWriter(0);

        String frontEndName = "java/lang/" + GENERATOR.getNextName(null);
        String frontTypeName = "L" + frontEndName + ";";
        String backendName = mirror.getName().replace('.', '/') + "$" + GENERATOR.getNextName(null);

        frontEndWriter.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                frontEndName, null,
                PubMagicAccessorImplJdkName == null ? "java/lang/Object" : PubMagicAccessorImplJdkName,
                null);
        backEndWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, backendName, null, frontEndName, null);
        frontEndWriter.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, "i", frontTypeName, null, null);

        MethodVisitor frontendClInit = frontEndWriter.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        frontendClInit.visitMaxs(10, 10);

        while (iterator.hasNext()) {
            Method method = iterator.next();
            RedirectInfos inf = method.getDeclaredAnnotation(RedirectInfos.class);
            if (inf == null) continue;
            for (RedirectInfos.Info info : inf.value()) {
                for (RedirectInfos.MethodInfo methodInfo : info.methods()) {
                    if (!checkMethodExists(methodInfo, info)) continue;
                    io.github.karlatemp.jhf.core.utils.RedirectInfos.RedirectInfo redirectInfo = new io.github.karlatemp.jhf.core.utils.RedirectInfos.RedirectInfo();
                    {
                        Class<?> target = info.value();
                        if (target == void.class) {
                            redirectInfo.sourceOwner = info.target();
                        } else {
                            redirectInfo.sourceOwner = target.getName().replace('.', '/');
                        }
                    }
                    redirectInfo.sourceMethodName = methodInfo.name();
                    redirectInfo.sourceMethodDesc = methodInfo.methodDesc();
                    redirectInfo.isStatic = methodInfo.invokeType() == InvokeType.invokeStatic;
                    if (redirectInfo.sourceMethodDesc.isEmpty()) {
                        redirectInfo.sourceMethodDesc = Type.getMethodDescriptor(
                                Type.getType(methodInfo.methodReturnType()),
                                Stream.of(methodInfo.methodParameters())
                                        .map(Type::getType)
                                        .toArray(Type[]::new)
                        );
                    }
                    current.add(redirectInfo);
                    Type retType = Type.getReturnType(redirectInfo.sourceMethodDesc);
                    Type[] argType;
                    String mirrorDesc;
                    {
                        Type[] tmp = Type.getArgumentTypes(redirectInfo.sourceMethodDesc);
                        if (redirectInfo.isStatic) {
                            argType = tmp;
                            mirrorDesc = redirectInfo.sourceMethodDesc;
                        } else {
                            argType = new Type[tmp.length + 1];
                            System.arraycopy(tmp, 0, argType, 1, tmp.length);
                            argType[0] = Type.getObjectType(redirectInfo.sourceOwner);
                            mirrorDesc = Type.getMethodDescriptor(retType, argType);
                        }
                    }
                    redirectInfo.methodName = metnameNG.getNextName(null);
                    redirectInfo.owner = frontEndName;

                    String absName = metnameNG.getNextName(null);

                    frontEndWriter.visitMethod(Opcodes.ACC_PROTECTED | Opcodes.ACC_ABSTRACT, absName, MTDESC, null, null);
                    {
                        MethodVisitor bkBg = backEndWriter.visitMethod(Opcodes.ACC_PROTECTED, absName, MTDESC, null, null);
                        bkBg.visitVarInsn(Opcodes.ALOAD, 1);
                        bkBg.visitTypeInsn(Opcodes.CHECKCAST, MIS);
                        bkBg.visitMethodInsn(Opcodes.INVOKESTATIC, mirror.getName().replace('.', '/'), method.getName(), RMTDESC, false);
                        bkBg.visitInsn(Opcodes.RETURN);
                        bkBg.visitMaxs(3, 3);
                    }

                    {
                        redirectInfo.methodDesc = mirrorDesc;
                        redirectInfo.methodName = metnameNG.getNextName(null);
                        MethodVisitor m_mirror = frontEndWriter.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, redirectInfo.methodName, mirrorDesc, null, null);
                        m_mirror.visitAnnotation(callerSensitive, true);
                        m_mirror.visitAnnotation(callerSensitive, false);
                        long[] ramSize = new long[2];
                        long[][] address = new long[1][];
                        MethodInvokeStackImpl.caclMemSize(ramSize, address, retType, argType);
                        String addrName = metnameNG.getNextName(null);
                        frontEndWriter.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, addrName, "[J", null, null);
                        {
                            long[] addr = address[0];
                            frontendClInit.visitLdcInsn(addr.length);
                            frontendClInit.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
                            frontendClInit.visitInsn(Opcodes.DUP);
                            frontendClInit.visitFieldInsn(Opcodes.PUTSTATIC, frontEndName, addrName, "[J");

                            for (int i = 0, ed = addr.length; i < ed; i++) {
                                frontendClInit.visitInsn(Opcodes.DUP);
                                frontendClInit.visitLdcInsn(i);
                                frontendClInit.visitLdcInsn(addr[i]);
                                frontendClInit.visitInsn(Opcodes.LASTORE);
                            }
                            frontendClInit.visitInsn(Opcodes.POP);
                        }

                        m_mirror.visitFieldInsn(Opcodes.GETSTATIC, frontEndName, "i", frontTypeName);

                        m_mirror.visitLdcInsn(ramSize[0]);
                        m_mirror.visitFieldInsn(Opcodes.GETSTATIC, frontEndName, addrName, "[J");
                        m_mirror.visitLdcInsn(argType.length);
                        m_mirror.visitLdcInsn((int) ramSize[1]);
                        m_mirror.visitMethodInsn(Opcodes.INVOKESTATIC, reflection, "getCallerClass", "()Ljava/lang/Class;", false);
                        m_mirror.visitMethodInsn(Opcodes.INVOKESTATIC, MethodInvokeStackJLAMirror.MIRROR_ALOC_NAME, "alloc", MethodInvokeStackJLAMirror.MIRROR_ALLOC_DESC, false);

                        int slot = 0;
                        {
                            for (Type argt : argType) {
                                m_mirror.visitInsn(Opcodes.DUP);
                                m_mirror.visitVarInsn(argt.getOpcode(Opcodes.ILOAD), slot);
                                slot += argt.getSize();
                                m_mirror.visitMethodInsn(
                                        Opcodes.INVOKEINTERFACE,
                                        MethodInvokeStackJLAMirror.JLA_MIRROR_CLASS_NAME,
                                        "emit",
                                        MethodInvokeStackJLAMirror.EMIT_METHOD_DESC[argt.getSort()],
                                        true
                                );
                            }
                            m_mirror.visitInsn(Opcodes.DUP);
                            m_mirror.visitVarInsn(Opcodes.ASTORE, slot);
                        }

                        Label tryStart = new Label(),
                                tryEnd = new Label(),
                                handler = new Label();
                        m_mirror.visitTryCatchBlock(tryStart, tryEnd, handler, "java/lang/Throwable");

                        m_mirror.visitLabel(tryStart);

                        m_mirror.visitMethodInsn(Opcodes.INVOKEVIRTUAL, frontEndName, absName, MTDESC, false);
                        {
                            m_mirror.visitVarInsn(Opcodes.ALOAD, slot);
                            m_mirror.visitMethodInsn(
                                    Opcodes.INVOKEINTERFACE,
                                    MethodInvokeStackJLAMirror.JLA_MIRROR_CLASS_NAME,
                                    "isReturned", "()Z", true
                            );
                            Label jumpIfNotReturn = new Label();
                            m_mirror.visitJumpInsn(Opcodes.IFEQ, jumpIfNotReturn);
                            if (retType.getSort() == Type.VOID) {

                                m_mirror.visitVarInsn(Opcodes.ALOAD, slot);
                                m_mirror.visitMethodInsn(
                                        Opcodes.INVOKEINTERFACE,
                                        MethodInvokeStackJLAMirror.JLA_MIRROR_CLASS_NAME,
                                        "release", "()V", true
                                );

                                m_mirror.visitInsn(Opcodes.RETURN);
                            } else {


                                m_mirror.visitVarInsn(Opcodes.ALOAD, slot);
                                m_mirror.visitMethodInsn(
                                        Opcodes.INVOKEINTERFACE,
                                        MethodInvokeStackJLAMirror.JLA_MIRROR_CLASS_NAME,
                                        MethodInvokeStackJLAMirror.POLL_METHOD_NAME[retType.getSort()],
                                        MethodInvokeStackJLAMirror.POLL_METHOD_DESC[retType.getSort()],
                                        true
                                );

                                m_mirror.visitVarInsn(Opcodes.ALOAD, slot);
                                m_mirror.visitMethodInsn(
                                        Opcodes.INVOKEINTERFACE,
                                        MethodInvokeStackJLAMirror.JLA_MIRROR_CLASS_NAME,
                                        "release", "()V", true
                                );

                                if (retType.getSort() == Type.OBJECT) {
                                    m_mirror.visitTypeInsn(Opcodes.CHECKCAST, retType.getInternalName());
                                } else if (retType.getSort() == Type.ARRAY) {
                                    m_mirror.visitTypeInsn(Opcodes.CHECKCAST, retType.getDescriptor());
                                }
                                m_mirror.visitInsn(retType.getOpcode(Opcodes.IRETURN));
                            }

                            m_mirror.visitLabel(jumpIfNotReturn);
                            m_mirror.visitVarInsn(Opcodes.ALOAD, slot);
                            m_mirror.visitMethodInsn(
                                    Opcodes.INVOKEINTERFACE,
                                    MethodInvokeStackJLAMirror.JLA_MIRROR_CLASS_NAME,
                                    "reset", "()V", true
                            );

                            for (Type argt : argType) {
                                m_mirror.visitVarInsn(Opcodes.ALOAD, slot);
                                m_mirror.visitMethodInsn(
                                        Opcodes.INVOKEINTERFACE,
                                        MethodInvokeStackJLAMirror.JLA_MIRROR_CLASS_NAME,
                                        MethodInvokeStackJLAMirror.POLL_METHOD_NAME[argt.getSort()],
                                        MethodInvokeStackJLAMirror.POLL_METHOD_DESC[argt.getSort()],
                                        true
                                );
                                if (argt.getSort() == Type.OBJECT) {
                                    m_mirror.visitTypeInsn(Opcodes.CHECKCAST, argt.getInternalName());
                                } else if (argt.getSort() == Type.ARRAY) {
                                    m_mirror.visitTypeInsn(Opcodes.CHECKCAST, argt.getDescriptor());
                                }
                            }

                            m_mirror.visitMethodInsn(
                                    redirectInfo.isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
                                    redirectInfo.sourceOwner,
                                    redirectInfo.sourceMethodName,
                                    redirectInfo.sourceMethodDesc,
                                    false
                            );
                            m_mirror.visitVarInsn(Opcodes.ALOAD, slot);
                            m_mirror.visitMethodInsn(
                                    Opcodes.INVOKEINTERFACE,
                                    MethodInvokeStackJLAMirror.JLA_MIRROR_CLASS_NAME,
                                    "release", "()V", true
                            );

                            if (retType.getSort() == Type.OBJECT) {
                                m_mirror.visitTypeInsn(Opcodes.CHECKCAST, retType.getInternalName());
                            } else if (retType.getSort() == Type.ARRAY) {
                                m_mirror.visitTypeInsn(Opcodes.CHECKCAST, retType.getDescriptor());
                            }
                            m_mirror.visitInsn(retType.getOpcode(Opcodes.IRETURN));
                        }
                        m_mirror.visitLabel(tryEnd);

                        {
                            m_mirror.visitLabel(handler);
                            m_mirror.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
                            m_mirror.visitVarInsn(Opcodes.ALOAD, slot);
                            m_mirror.visitMethodInsn(
                                    Opcodes.INVOKEINTERFACE,
                                    MethodInvokeStackJLAMirror.JLA_MIRROR_CLASS_NAME,
                                    "release", "()V", true
                            );
                            m_mirror.visitInsn(Opcodes.DUP);
                            m_mirror.visitMethodInsn(Opcodes.INVOKESTATIC, MethodInvokeStackJLAMirror.MIRROR_ALOC_NAME, "hs", "(Ljava/lang/Throwable;)V", false);
                            m_mirror.visitInsn(Opcodes.ATHROW);
                        }

                        m_mirror.visitMaxs(slot + 10, slot + 3);
                    }
                }
            }
        }

        frontendClInit.visitInsn(Opcodes.RETURN);

        if (current.isEmpty()) return;
        redirectInfos.addAll(current);
        for (io.github.karlatemp.jhf.core.utils.RedirectInfos.RedirectInfo red : current) {
            StackReMapInfo.register(
                    red.owner.replace('/', '.'),
                    red.methodName,
                    red.sourceOwner.replace('/', '.'),
                    red.sourceMethodName
            );
        }
        {
            byte[] b = frontEndWriter.toByteArray(), bkx = backEndWriter.toByteArray();
            Unsafe unsafe = UAAccessHolder.UNSAFE;
            Class<?> t = DmpC.define(null, b);
            ExtendsForbidden.DENIED.add(t.getName().replace('.', '/'));
            Class<?> dg = DmpC.defineAnonymous(mirror, bkx);
            ExtendsForbidden.DENIED.add(dg.getName().replace('.', '/'));
            try {
                Field f = t.getDeclaredField("i");
                unsafe.putReference(
                        unsafe.staticFieldBase(f),
                        unsafe.staticFieldOffset(f),
                        unsafe.allocateInstance(dg)
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        for (io.github.karlatemp.jhf.core.utils.RedirectInfos.RedirectInfo red : current) {
            try {
                MagicAccessorGenerator.gen(red);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private static boolean checkMethodExists(RedirectInfos.MethodInfo methodInfo, RedirectInfos.Info info) {
        try {
            Class<?> c = info.value();
            if (c == void.class) {
                c = Class.forName(info.target().replace('/', '.'));
            }
            MethodHandles.Lookup ROOT = UAAccessHolder.UA.getTrustedIn(c);
            String desc = methodInfo.methodDesc();
            MethodType mt;
            if (desc.isEmpty()) {
                mt = MethodType.methodType(methodInfo.methodReturnType(), methodInfo.methodParameters());
            } else {
                mt = MethodType.fromMethodDescriptorString(desc, null);
            }
            String name = methodInfo.name();
            if (methodInfo.invokeType() == InvokeType.invokeStatic) {
                ROOT.findStatic(c, name, mt);
            } else {
                ROOT.findVirtual(c, name, mt);
            }
            return true;
        } catch (NoSuchMethodException | NoClassDefFoundError | ClassNotFoundException | TypeNotPresentException ignore) {
            return false;
        } catch (Throwable throwable) {
            MxLib.getLogger().warn(throwable);
            return false;
        }
    }
}
