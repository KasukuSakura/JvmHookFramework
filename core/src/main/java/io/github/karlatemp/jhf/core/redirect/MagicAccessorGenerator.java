package io.github.karlatemp.jhf.core.redirect;

import io.github.karlatemp.jhf.api.event.EventPriority;
import io.github.karlatemp.jhf.api.events.ReflectionAccessorGenerateEvent;
import io.github.karlatemp.jhf.api.events.ReflectionAccessorPreGenerateEvent;
import io.github.karlatemp.jhf.api.utils.MapMirroredSet;
import io.github.karlatemp.jhf.api.utils.NameGenerator;
import io.github.karlatemp.jhf.api.utils.NonRepeatingNameGenerator;
import io.github.karlatemp.jhf.api.utils.RandomNameGenerator;
import io.github.karlatemp.jhf.core.utils.ASMUtils;
import io.github.karlatemp.jhf.core.utils.DmpC;
import io.github.karlatemp.jhf.core.utils.RedirectInfos;
import io.github.karlatemp.jhf.core.utils.UAAccessHolder;
import org.objectweb.asm.Type;
import org.objectweb.asm.*;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MagicAccessorGenerator {
    private static final ClassLoader CC = new ClassLoader(MagicAccessorGenerator.class.getClassLoader()) {
    };

    private interface Handler {
        void handle(ReflectionAccessorPreGenerateEvent event, String desc);
    }

    private interface HandlerMG {
        void handle(ReflectionAccessorGenerateEvent event, String desc);
    }


    private static final Collection<Handler> handlers = new ConcurrentLinkedDeque<>();
    private static final Collection<HandlerMG> mg_handlers = new ConcurrentLinkedDeque<>();

    static {
        ReflectionAccessorPreGenerateEvent.EVENT_LINE.register(EventPriority.NORMAL, event -> {
            Member m = event.member;
            String desc;
            if (m instanceof Method) {
                desc = Type.getMethodDescriptor((Method) m);
            } else if (m instanceof Field) {
                desc = Type.getType(((Field) m).getType()).getDescriptor();
            } else {
                desc = Type.getConstructorDescriptor((Constructor<?>) m);
            }
            for (Handler h : handlers) {
                h.handle(event, desc);
            }
        });
        ReflectionAccessorGenerateEvent.EVENT_LINE.register(EventPriority.NORMAL, event -> {
            Member m = event.requested;
            String desc;
            if (m instanceof Method) {
                desc = Type.getMethodDescriptor((Method) m);
            } else if (m instanceof Field) {
                desc = Type.getType(((Field) m).getType()).getDescriptor();
            } else {
                desc = Type.getConstructorDescriptor((Constructor<?>) m);
            }
            for (HandlerMG h : mg_handlers) {
                h.handle(event, desc);
            }
        });
    }

    private static final String

            ACCESSOR_IMPL_PACKAGE = "io/github/karlatemp/jhf/api/markers/",
            METHOD_ACCESSOR_IMPL = ACCESSOR_IMPL_PACKAGE + "MethodAccessorImpl",
            FIELD_ACCESSOR_IMPL = ACCESSOR_IMPL_PACKAGE + "FieldAccessorImpl",
            CONSTRUCTOR_ACCESSOR_IMPL = ACCESSOR_IMPL_PACKAGE + "ConstructorAccessorImpl";

    private static final NameGenerator GEN = new NonRepeatingNameGenerator(
            new MapMirroredSet<>(new ConcurrentHashMap<>()),
            RandomNameGenerator.INSTANCE,
            5
    );
    public static String pkgPrefix = "jdk/internal/reflect/RR_ACC$Z$";

    private static void registerRemap(RedirectInfos.RedirectInfo info) throws Throwable {
        Class<?> src = Class.forName(info.owner.replace('/', '.')),
                crc = Class.forName(info.sourceOwner.replace('/', '.'));
        Method method = null;
        for (Method m : src.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (!m.getName().equals(info.methodName)) continue;
            if (!Type.getMethodDescriptor(m).equals(info.methodDesc)) continue;
            method = m;
        }
        if (method == null)
            throw new AssertionError("No method of " + info + " found");
        final Method finalMethod = method;

        handlers.add((event, desc) -> {
            Member member = event.member;
            if (member.getDeclaringClass() != crc) return;
            if (member instanceof Method) {
                Method methodX = (Method) member;
                if (methodX.getName().equals(info.sourceMethodName)) {
                    if (desc.equals(info.methodDesc)) {
                        event.member = finalMethod;
                    }
                }
            }
        });
    }

    public static void wrap(MethodVisitor visitor, Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case Type.CHAR:
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case Type.BYTE:
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case Type.DOUBLE:
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            case Type.FLOAT:
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case Type.INT:
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case Type.LONG:
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case Type.SHORT:
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
        }
    }

    public static void unwrap(MethodVisitor visitor, Type type) {
        int sort = type.getSort();
        if (sort == Type.ARRAY) {
            visitor.visitTypeInsn(Opcodes.CHECKCAST, type.getDescriptor());
        } else if (sort == Type.OBJECT) {
            if (type.getInternalName().equals("java/lang/Object")) return;
            visitor.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
        } else if (sort == Type.BOOLEAN) {
            visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
        } else if (sort == Type.CHAR) {
            visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
        } else {
            visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
        }
        switch (sort) {
            case Type.BOOLEAN:
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                break;
            case Type.CHAR:
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                break;
            case Type.BYTE:
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "byteValue", "()B", false);
                break;
            case Type.DOUBLE:
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                break;
            case Type.FLOAT:
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
                break;
            case Type.INT:
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
                break;
            case Type.LONG:
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
                break;
            case Type.SHORT:
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "shortValue", "()S", false);
                break;
        }
    }

    public static void gen(RedirectInfos.RedirectInfo info) throws Throwable {
        Class<?> src = Class.forName(info.sourceOwner.replace('/', '.'));
        if (info.isStatic) {
            registerRemap(info);
            return;
        }
        ClassWriter writer = new ClassWriter(0);
        boolean isCtr = info.sourceMethodName.equals("<init>");
        String name = pkgPrefix + (isCtr ? "ConstructorAccessor$$" : "MethodAccessor$$") + GEN.getNextName(null);
        writer.visit(Opcodes.V1_8, 0, name, null,
                isCtr ? CONSTRUCTOR_ACCESSOR_IMPL : METHOD_ACCESSOR_IMPL,
                null
        );


        MethodVisitor visitor = writer.visitMethod(
                Opcodes.ACC_PUBLIC,
                isCtr ? "newInstance" : "invoke",
                isCtr ? "([Ljava/lang/Object;)Ljava/lang/Object;" : "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
        );

        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label handlerStart = new Label();

        visitor.visitTryCatchBlock(tryStart, tryEnd, handlerStart, "java/lang/Throwable");

        Type[] args = Type.getArgumentTypes(info.methodDesc);
        if (!isCtr) { // this null check
            visitor.visitVarInsn(Opcodes.ALOAD, 1);
            Label end = new Label();
            visitor.visitJumpInsn(Opcodes.IFNONNULL, end);

            visitor.visitTypeInsn(Opcodes.NEW, "java/lang/NullPointerException");
            visitor.visitInsn(Opcodes.DUP);
            visitor.visitLdcInsn("`this` is null");
            visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "(Ljava/lang/String;)V", false);
            visitor.visitInsn(Opcodes.ATHROW);
            visitor.visitLabel(end);
        }
        if (args.length != (isCtr ? 0 : 1)) { // check argument size
            int argumentsSlot = isCtr ? 1 : 2;
            visitor.visitVarInsn(Opcodes.ALOAD, argumentsSlot);
            Label end = new Label();
            visitor.visitJumpInsn(Opcodes.IFNONNULL, end);

            visitor.visitTypeInsn(Opcodes.NEW, "java/lang/NullPointerException");
            visitor.visitInsn(Opcodes.DUP);
            visitor.visitLdcInsn("`arguments` is null");
            visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "(Ljava/lang/String;)V", false);
            visitor.visitInsn(Opcodes.ATHROW);

            visitor.visitLabel(end);


            visitor.visitVarInsn(Opcodes.ALOAD, argumentsSlot);
            visitor.visitInsn(Opcodes.ARRAYLENGTH);
            ASMUtils.emit(visitor, args.length - (isCtr ? 0 : 1));
            Label ok = new Label();
            visitor.visitJumpInsn(Opcodes.IF_ICMPEQ, ok);

            visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
            visitor.visitInsn(Opcodes.DUP);
            visitor.visitLdcInsn("wrong arguments size");
            visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
            visitor.visitInsn(Opcodes.ATHROW);

            visitor.visitLabel(ok);
        }

        int size = 1;
        if (!isCtr) {
            visitor.visitVarInsn(Opcodes.ALOAD, 1);
            unwrap(visitor, args[0]);
        }

        for (int i = (isCtr ? 0 : 1), ed = args.length; i < ed; i++) {
            visitor.visitVarInsn(Opcodes.ALOAD, isCtr ? 1 : 2);
            ASMUtils.emit(visitor, i - (isCtr ? 0 : 1));
            visitor.visitInsn(Opcodes.AALOAD);
            size += args[i].getSize();
            unwrap(visitor, args[i]);
        }

        visitor.visitLabel(tryStart);
        visitor.visitMethodInsn(Opcodes.INVOKESTATIC, info.owner, info.methodName, info.methodDesc, false);

        Type ret = Type.getReturnType(info.methodDesc);
        if (ret.getSort() == Type.VOID) {
            visitor.visitInsn(Opcodes.ACONST_NULL);
        } else {
            wrap(visitor, ret);
        }
        visitor.visitInsn(Opcodes.ARETURN);

        visitor.visitLabel(tryEnd);
        visitor.visitLabel(handlerStart);

        visitor.visitVarInsn(Opcodes.ASTORE, 1);
        // java/lang/reflect/InvocationTargetException
        visitor.visitTypeInsn(Opcodes.NEW, "java/lang/reflect/InvocationTargetException");
        visitor.visitInsn(Opcodes.DUP);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/reflect/InvocationTargetException", "<init>", "(Ljava/lang/Throwable;)V", false);
        visitor.visitInsn(Opcodes.ATHROW);

        visitor.visitMaxs(size + 3, 4);

        byte[] code = writer.toByteArray();
        // DmpC.dump(code);
        Class<?> accessorC = DmpC.define(CC, code);
//        Class<?> accessorC = UAAccessHolder.UNSAFE.defineClass(null, code, 0, code.length, CC, null);
        Object accessorI = UAAccessHolder.UNSAFE.allocateInstance(accessorC);

        mg_handlers.add((event, desc) -> {
            Member requested = event.requested;
            if (isCtr) {
                if (!(requested instanceof Constructor<?>)) return;
            } else {
                if (!(requested instanceof Method)) return;
            }
            if (requested.getDeclaringClass() != src) return;
            if (Modifier.isStatic(requested.getModifiers())) return;
            if (!desc.equals(info.sourceMethodDesc)) return;
            event.response = accessorI;
        });
    }
}
