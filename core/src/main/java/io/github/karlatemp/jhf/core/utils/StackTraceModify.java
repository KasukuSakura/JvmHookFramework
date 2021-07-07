package io.github.karlatemp.jhf.core.utils;

import io.github.karlatemp.unsafeaccessor.Root;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class StackTraceModify {
    public static final String
            declaringClass,
            methodName,
            fileName,
            lineNumber;

    public static final MethodHandles.Lookup ROOT_IN_STACK_TRACE_ELM = Root.getTrusted(StackTraceElement.class);

    public static final MethodHandle
            set$declaringClass,
            set$methodName,
            set$fileName,
            set$lineNumber;

    private static String findFieldAccess(ClassNode node, String metName, String metDesc) {
        for (MethodNode m : node.methods) {
            if (m.instructions == null) continue;
            if (m.desc.equals(metDesc)) {
                if (m.name.equals(metName)) {
                    for (AbstractInsnNode ins : m.instructions) {
                        if (ins instanceof FieldInsnNode) {
                            return ((FieldInsnNode) ins).name;
                        }
                    }
                }
            }
        }
        throw new NoSuchFieldError("No field access in " + node.name + "." + metName + metDesc);
    }

    static {
        try {
            ClassNode node = new ClassNode();
            new ClassReader("java.lang.StackTraceElement")
                    .accept(node, 0);
            declaringClass = findFieldAccess(node, "getClassName", "()Ljava/lang/String;");
            methodName = findFieldAccess(node, "getMethodName", "()Ljava/lang/String;");
            fileName = findFieldAccess(node, "getFileName", "()Ljava/lang/String;");
            lineNumber = findFieldAccess(node, "getLineNumber", "()I");

            set$declaringClass = ROOT_IN_STACK_TRACE_ELM.findSetter(
                    StackTraceElement.class, declaringClass, String.class
            );
            set$methodName = ROOT_IN_STACK_TRACE_ELM.findSetter(
                    StackTraceElement.class, methodName, String.class
            );
            set$fileName = ROOT_IN_STACK_TRACE_ELM.findSetter(
                    StackTraceElement.class, fileName, String.class
            );
            set$lineNumber = ROOT_IN_STACK_TRACE_ELM.findSetter(
                    StackTraceElement.class, lineNumber, int.class
            );
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
