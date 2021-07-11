package io.github.karlatemp.jhf.core.utils;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ASMUtils {
    public static void emit(MethodVisitor visitor, int value) {
        if (value >= -1 && value <= 5) {
            visitor.visitInsn(Opcodes.ICONST_0 + value);
        } else {
            visitor.visitLdcInsn(value);
        }
    }

    public static void emit(MethodVisitor visitor, long value) {
        if (value >= 0 && value <= 1) {
            visitor.visitInsn(Opcodes.LCONST_0 + (int) value);
        } else {
            visitor.visitLdcInsn(value);
        }
    }
}
