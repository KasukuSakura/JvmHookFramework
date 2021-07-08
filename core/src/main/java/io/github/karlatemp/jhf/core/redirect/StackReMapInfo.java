package io.github.karlatemp.jhf.core.redirect;

import io.github.karlatemp.jhf.api.utils.ClassFinder;
import io.github.karlatemp.jhf.api.utils.SneakyThrow;
import io.github.karlatemp.jhf.core.utils.RedirectInfos;
import io.github.karlatemp.jhf.core.utils.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
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

    /**
     * Source method to obf method
     */
    public static Method remap(Method src) throws Exception {
        Class<?> declaringClass = src.getDeclaringClass();
        String iname = declaringClass.getName().replace('.', '/');
        String dsc = Type.getMethodDescriptor(src);
        for (RedirectInfos.RedirectInfo redirectInfo : RedirectGenerator.redirectInfos) {
            if (redirectInfo.isStatic != Modifier.isStatic(src.getModifiers())) continue;
            if (!redirectInfo.sourceOwner.equals(iname)) continue;
            if (!redirectInfo.sourceMethodName.equals(src.getName())) continue;
            if (!redirectInfo.sourceMethodDesc.equals(dsc)) continue;

            if (!redirectInfo.isStatic) {
                throw new IllegalAccessException("Cannot access " + src + " by reflection");
            }

            Class<?> delegate = Class.forName(redirectInfo.owner.replace('/', '.'), false, null);
            for (Method method : delegate.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    if (method.getName().equals(redirectInfo.methodName)) {
                        if (Type.getMethodDescriptor(method).equals(redirectInfo.methodDesc)) {
                            // UAAccessHolder.UA.setAccessible(method, src.isAccessible());
                            System.out.println("Redirected to " + method);
                            return method;
                        }
                    }
                }
            }
        }
        return src;
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
        newMethodAccessor:
        {
            for (MethodNode method : cn.methods) {
                if (method.name.equals("newMethodAccessor")) {
                    InsnList al = new InsnList();
                    al.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    al.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            MethodInvokeStackJLAMirror.MIRROR_ALOC_NAME,
                            MethodInvokeStackJLAMirror.REMAP_METHOD_M_NAME,
                            MethodInvokeStackJLAMirror.REMAP_METHOD_M_DESC, false
                    ));
                    al.add(new VarInsnNode(Opcodes.ASTORE, 1));

                    method.instructions.insert(al);
                    break newMethodAccessor;
                }
            }
            throw new IllegalClassFormatException("No newMethodAccessor in bytecode of " + RF);
        }
        byte[] code = DmpC.toByteCode(cn);
        instrumentation.redefineClasses(
                new ClassDefinition(RF, code)
        );
    }
}
