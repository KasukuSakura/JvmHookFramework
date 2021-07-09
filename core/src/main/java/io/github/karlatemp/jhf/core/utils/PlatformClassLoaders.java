package io.github.karlatemp.jhf.core.utils;

public class PlatformClassLoaders {
    public ClassLoader platformClassLoader() {
        return ClassLoader.getSystemClassLoader().getParent();
    }
}
