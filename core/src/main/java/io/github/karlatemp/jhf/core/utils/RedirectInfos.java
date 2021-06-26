package io.github.karlatemp.jhf.core.utils;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Collection;

public class RedirectInfos {
    public static class RedirectInfo {
        public String owner;
        public String methodName;
        public String methodDesc;

        public String sourceOwner;
        public String sourceMethodName;
        public String sourceMethodDesc;
        public boolean isStatic;

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
                    '}';
        }
    }

    public static void applyRedirect(ClassNode classNode, Collection<RedirectInfo> redirectInfos) {
        if (redirectInfos == null || redirectInfos.isEmpty()) return;
        for (MethodNode method : classNode.methods) {
            applyRedirect(method, redirectInfos);
        }
    }

    private static RedirectInfo findRedirectInfo(Collection<RedirectInfo> rd, String own, String name, String desc, boolean isStatic) {
        for (RedirectInfo redirectInfo : rd) {
            if (redirectInfo.isStatic ^ isStatic) continue;
            if (!redirectInfo.sourceOwner.equals(own)) continue;
            if (!redirectInfo.sourceMethodName.equals(name)) continue;
            if (!redirectInfo.sourceMethodDesc.equals(desc)) continue;
            return redirectInfo;
        }
        return null;
    }

    public static void applyRedirect(MethodNode methodNode, Collection<RedirectInfo> redirectInfos) {
        if (redirectInfos == null || redirectInfos.isEmpty()) return;
        InsnList instructions = methodNode.instructions;
        if (instructions == null) return;
        for (AbstractInsnNode insnNode : instructions) {
            if (insnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                RedirectInfo red = findRedirectInfo(redirectInfos, methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC);
                if (red != null) {
                    methodInsnNode.owner = red.owner;
                    methodInsnNode.name = red.methodName;
                    methodInsnNode.desc = red.methodDesc;
                    methodInsnNode.itf = false;
                    methodInsnNode.setOpcode(Opcodes.INVOKESTATIC);
                }
            } else if (insnNode instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode invokeDynamicInsnNode = (InvokeDynamicInsnNode) insnNode;
                invokeDynamicInsnNode.bsm = applyRedirect(invokeDynamicInsnNode.bsm, redirectInfos);

                Object[] bsmArgs = invokeDynamicInsnNode.bsmArgs;
                for (int i = 0, bsmArgsLength = bsmArgs.length; i < bsmArgsLength; i++) {
                    Object arg = bsmArgs[i];
                    if (arg instanceof Handle) {
                        bsmArgs[i] = applyRedirect((Handle) arg, redirectInfos);
                    }
                }
            }
        }
    }

    public static Handle applyRedirect(Handle bsm, Collection<RedirectInfo> redirectInfos) {
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
        return bsm;
    }
}
