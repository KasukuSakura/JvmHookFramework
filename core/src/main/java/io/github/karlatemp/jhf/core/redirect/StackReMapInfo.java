package io.github.karlatemp.jhf.core.redirect;

import io.github.karlatemp.jhf.api.utils.ClassFinder;
import io.github.karlatemp.jhf.api.utils.SneakyThrow;
import io.github.karlatemp.jhf.core.utils.DmpC;
import io.github.karlatemp.jhf.core.utils.MethodInvokeStackJLAMirror;
import io.github.karlatemp.jhf.core.utils.StackTraceModify;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

public class StackReMapInfo {
    public static class Inf {
        public String on, omn, sn, smn, fn;
    }

    public static final Collection<Inf> infs = new ConcurrentLinkedDeque<>();

    public static void register(String obf, String obfN, String sn, String smn) {
        Inf inf = new Inf();
        inf.on = obf;
        inf.omn = obfN;
        inf.sn = sn;
        inf.smn = smn;
        {
            int llf = sn.lastIndexOf('.');
            if (llf == -1) {
                inf.fn = llf + ".java";
            } else {
                inf.fn = sn.substring(llf + 1) + ".java";
            }
        }
        infs.add(inf);
    }

    public static void remap(StackTraceElement element) {
        Inf fd = null;
        for (Inf inf : infs) {
            if (inf.on.equals(element.getClassName())) {
                if (inf.omn.equals(element.getMethodName())) {
                    fd = inf;
                    break;
                }
            }
        }
        if (fd == null) return;
        try {
            StackTraceModify.set$declaringClass.invoke(element, fd.sn);
            StackTraceModify.set$methodName.invoke(element, fd.smn);
            StackTraceModify.set$fileName.invoke(element, fd.fn);
            // StackTraceModify.set$lineNumber.invoke(element, -1);
        } catch (Throwable throwable) {
            SneakyThrow.throw0(throwable);
        }
    }

    public static void refineReflectionFactory(Instrumentation instrumentation) throws Exception {
        Class<?> RF = ClassFinder.findClass(null,
                "jdk.internal.reflect.ReflectionFactory",
                "sun.reflect.ReflectionFactory"
        );
        if (!instrumentation.isModifiableClass(RF)) {
            throw new IllegalStateException("Cannot modify " + RF);
        }
        ClassNode cn = new ClassNode();
        new ClassReader(RF.getName()).accept(cn, 0);

        {
            Set<String> names = new HashSet<>();
            names.add("newConstructorAccessor");
            names.add("newMethodAccessor");
            names.add("newFieldAccessor");
            // check
            {
                Set<String> copy = new HashSet<>(names);
                for (MethodNode method : cn.methods) {
                    copy.remove(method.name);
                }
                if (!copy.isEmpty()) {
                    throw new IllegalClassFormatException("No " + copy + " in bytecode of " + RF);
                }
            }
            for (MethodNode method : cn.methods) {
                if (names.contains(method.name)) {
                    InsnList al = new InsnList();
                    al.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    al.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            MethodInvokeStackJLAMirror.MIRROR_ALOC_NAME,
                            MethodInvokeStackJLAMirror.REMAP_METHOD_M_NAME,
                            MethodInvokeStackJLAMirror.REMAP_METHOD_M_DESC, false
                    ));
                    Label ifNull = new Label();
                    al.add(new InsnNode(Opcodes.DUP));
                    al.add(new JumpInsnNode(Opcodes.IFNULL, new LabelNode(ifNull)));
                    al.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getReturnType(method.desc).getInternalName()));
                    al.add(new InsnNode(Opcodes.ARETURN));
                    al.add(new LabelNode(ifNull));
                    al.add(new InsnNode(Opcodes.POP));
                    method.instructions.insert(al);
                }
            }
        }
        byte[] code = DmpC.toByteCode(cn);
        instrumentation.redefineClasses(
                new ClassDefinition(RF, code)
        );
    }
}
