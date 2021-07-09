package io.github.karlatemp.jhf.core.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.obfuscation.RemapperChain;
import org.spongepowered.asm.service.IClassBytecodeProvider;

import java.io.IOException;
import java.io.InputStream;

public class JHFBytecodeProvider implements IClassBytecodeProvider {
    private static class RemapperX extends Remapper {
        private final RemapperChain remapperChain = MixinEnvironment.getCurrentEnvironment().getRemappers();

        @Override
        public String map(String internalName) {
            return remapperChain.map(internalName);
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            return remapperChain.mapMethodName(owner, name, descriptor);
        }

        @Override
        public String mapDesc(String methodDescriptor) {
            return remapperChain.mapDesc(methodDescriptor);
        }

        @Override
        public String mapSignature(String signature, boolean typeSignature) {
            try {
                return super.mapSignature(signature, typeSignature);
            } catch (Throwable ignore) {
                return signature;
            }
        }
    }

    private static InputStream findC(String name) {
        InputStream resp = JHFClassProvider.CCL.getResourceAsStream(name);
        if (resp != null) return resp;

        // resp = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
        // if (resp != null) return resp;

        ClassLoader cc_in_trans = CC_IN_TRANS;
        if (cc_in_trans != null) {
            resp = cc_in_trans.getResourceAsStream(name);
        }

        return resp;
    }

    public static ClassLoader CC_IN_TRANS;
    public static String LOADING_N;
    public static byte[] LOADING_C;

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        RemapperX rm;
        if (runTransformers) {
            rm = new RemapperX();
            name = rm.map(name.replace('.', '/'));
        } else {
            rm = null;
        }
        if (LOADING_N != null && LOADING_C != null) {
            if (LOADING_N.equals(name) || LOADING_N.equals(name.replace('.', '/'))) {
                ClassNode nd = new ClassNode();
                new ClassReader(LOADING_C).accept(
                        runTransformers
                                ? new ClassRemapper(nd, rm)
                                : nd,
                        0);
                return nd;
            }
        }
        try (InputStream rs = findC(name.replace('.', '/') + ".class")) {
            if (rs == null) throw new ClassNotFoundException(name);
            ClassNode nd = new ClassNode();
            new ClassReader(rs).accept(
                    runTransformers
                            ? new ClassRemapper(nd, rm)
                            : nd,
                    0);
            return nd;
        }
    }
}
