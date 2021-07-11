package io.github.karlatemp.jhf.core.utils;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.invoke.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RedirectInfos {
    public static final Collection<RedirectInfo> redirectInfos = new ConcurrentLinkedDeque<>();


    public static class RedirectInfo {
        public String owner;
        public String methodName;
        public String methodDesc;

        public String sourceOwner;
        public String sourceMethodName;
        public String sourceMethodDesc;
        public boolean isStatic;
        public boolean isSourceFinal;

        public RedirectInfo(
                String owner, String methodName, String methodDesc,
                String sourceOwner, String sourceMethodName, String sourceMethodDesc,
                boolean isStatic
        ) {
            this.owner = owner;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.sourceOwner = sourceOwner;
            this.sourceMethodName = sourceMethodName;
            this.sourceMethodDesc = sourceMethodDesc;
            this.isStatic = isStatic;
        }

        public RedirectInfo() {
        }

        @Override
        public String toString() {
            return "RedirectInfo{" +
                    "owner='" + owner + '\'' +
                    ", methodName='" + methodName + '\'' +
                    ", methodDesc='" + methodDesc + '\'' +
                    ", sourceOwner='" + sourceOwner + '\'' +
                    ", sourceMethodName='" + sourceMethodName + '\'' +
                    ", sourceMethodDesc='" + sourceMethodDesc + '\'' +
                    ", isStatic=" + isStatic +
                    ", isSourceFinal=" + isSourceFinal +
                    '}';
        }
    }

    public static void applyRedirect(ClassNode classNode, Collection<RedirectInfo> redirectInfos) {
        if (redirectInfos == null || redirectInfos.isEmpty()) return;
        for (MethodNode method : new ArrayList<>(classNode.methods)) {
            applyRedirect(classNode, method, redirectInfos);
        }
    }

    public static RedirectInfo findRedirectInfo(Collection<RedirectInfo> rd, String own, String name, String desc, boolean isStatic) {
        for (RedirectInfo redirectInfo : rd) {
            if (redirectInfo.isStatic ^ isStatic) continue;
            if (!redirectInfo.sourceOwner.equals(own)) continue;
            if (!redirectInfo.sourceMethodName.equals(name)) continue;
            if (!redirectInfo.sourceMethodDesc.equals(desc)) continue;
            return redirectInfo;
        }
        return null;
    }

    public static void applyRedirect(ClassNode classNode, MethodNode methodNode, Collection<RedirectInfo> redirectInfos) {
        if (redirectInfos == null || redirectInfos.isEmpty()) return;
        InsnList instructions = methodNode.instructions;
        if (instructions == null) return;
        // insnLoop:
        for (ListIterator<AbstractInsnNode> iterator = instructions.iterator(); iterator.hasNext(); ) {
            AbstractInsnNode insnNode = iterator.next();
            if (insnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                RedirectInfo red = findRedirectInfo(redirectInfos, methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC);
                if (red != null) {
                    methodInsnNode.owner = red.owner;
                    methodInsnNode.name = red.methodName;
                    methodInsnNode.desc = red.methodDesc;
                    methodInsnNode.itf = false;
                    methodInsnNode.setOpcode(Opcodes.INVOKESTATIC);
                    continue;
                }
                if (methodInsnNode.getOpcode() != Opcodes.INVOKESTATIC) continue;
                if (!hasSimilar(redirectInfos, methodInsnNode.name, methodInsnNode.desc)) continue;
                iterator.set(new InvokeDynamicInsnNode(
                        "run",
                        methodInsnNode.desc,
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                MethodInvokeStackJLAMirror.MIRROR_ALOC_NAME,
                                MethodInvokeStackJLAMirror.MH_DYN_C_NAME,
                                MethodInvokeStackJLAMirror.MH_DYN_C_DESC,
                                false
                        ),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                methodInsnNode.owner,
                                methodInsnNode.name,
                                methodInsnNode.desc,
                                methodInsnNode.itf
                        )
                ));
            } else if (insnNode instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode invokeDynamicInsnNode = (InvokeDynamicInsnNode) insnNode;
                invokeDynamicInsnNode.bsm = applyRedirect(classNode, invokeDynamicInsnNode.bsm, redirectInfos);

                Object[] bsmArgs = invokeDynamicInsnNode.bsmArgs;
                for (int i = 0, bsmArgsLength = bsmArgs.length; i < bsmArgsLength; i++) {
                    Object arg = bsmArgs[i];
                    if (arg instanceof Handle) {
                        bsmArgs[i] = applyRedirect(classNode, (Handle) arg, redirectInfos);
                    }
                }
            }
        }
    }

    public static Handle applyRedirect(
            ClassNode klass,
            Handle bsm,
            Collection<RedirectInfo> redirectInfos
    ) {
        RedirectInfo redirectInfo = findRedirectInfo(redirectInfos, bsm.getOwner(), bsm.getName(), bsm.getDesc(), bsm.getTag() == Opcodes.H_INVOKESTATIC);
        if (redirectInfo != null) {
            return new Handle(
                    Opcodes.H_INVOKESTATIC,
                    redirectInfo.owner,
                    redirectInfo.methodName,
                    redirectInfo.methodDesc,
                    false
            );
        }
        if (bsm.getTag() != Opcodes.H_INVOKESTATIC) {
            return bsm;
        }
        if (hasSimilar(redirectInfos, bsm.getName(), bsm.getDesc())) {
            String name = "ffr$tmp$b$" + klass.methods.size();
            MethodVisitor bridge = klass.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, name, bsm.getDesc(), null, null);

            int size = 1;
            for (Type type : Type.getArgumentTypes(bsm.getDesc())) {
                bridge.visitVarInsn(type.getOpcode(Opcodes.ILOAD), size);
                size += type.getSize();
            }
            bridge.visitInvokeDynamicInsn(
                    "run", bsm.getDesc(),
                    new Handle(
                            Opcodes.H_INVOKESTATIC,
                            MethodInvokeStackJLAMirror.MIRROR_ALOC_NAME,
                            MethodInvokeStackJLAMirror.MH_DYN_C_NAME,
                            MethodInvokeStackJLAMirror.MH_DYN_C_DESC,
                            false
                    ), bsm
            );
            bridge.visitInsn(Type.getReturnType(bsm.getDesc()).getOpcode(Opcodes.IRETURN));
            bridge.visitMaxs(size + 1, size + 1);
            return new Handle(
                    Opcodes.H_INVOKESTATIC,
                    klass.name,
                    name,
                    bsm.getDesc(),
                    false
            );
        }
        return bsm;
    }

    private static boolean hasSimilar(Collection<RedirectInfo> redirectInfos, String name, String desc) {
        for (RedirectInfo info : redirectInfos) {
            if (!info.isStatic) continue;
            if (info.isSourceFinal) continue;
            if (!info.sourceMethodName.equals(name)) continue;
            if (!info.sourceMethodDesc.equals(desc)) continue;
            return true;
        }
        return false;
    }

    // Only accept static
    public static CallSite redirect(
            MethodHandles.Lookup caller,
            String invokedName,
            MethodType type,
            MethodHandle sourceHandle
    ) throws Throwable {
        MethodHandleInfo target = caller.revealDirect(sourceHandle);
        if (target.getReferenceKind() != MethodHandleInfo.REF_invokeStatic) {
            // Skip
            return new ConstantCallSite(sourceHandle);
        }
        String in = target.getDeclaringClass().getName().replace('.', '/');
        String desc = target.getMethodType().toMethodDescriptorString();
        for (RedirectInfo info : redirectInfos) {
            if (info.isSourceFinal) continue;
            if (!info.isStatic) continue;

            if (!info.sourceOwner.equals(in)) continue;
            if (!info.sourceMethodName.equals(target.getName())) continue;
            if (!info.sourceMethodDesc.equals(desc)) continue;

            return new ConstantCallSite(caller.findStatic(
                    Class.forName(info.owner.replace('/', '.'), false, null),
                    info.methodName,
                    target.getMethodType()
            ));
        }

        return new ConstantCallSite(sourceHandle);
    }
}
