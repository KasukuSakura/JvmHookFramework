package io.github.karlatemp.jhf.core.mixin;

import org.spongepowered.asm.service.IClassProvider;

import java.net.URL;

public class JHFClassProvider implements IClassProvider {
    JHFClassProvider() {
    }

    static ClassLoader CCL = JHFClassProvider.class.getClassLoader();

    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, CCL);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return null;
    }
}
