package io.github.karlatemp.jhf.core.utils;

public class PlatformClassLoaders9 extends PlatformClassLoaders {
    @Override
    public ClassLoader platformClassLoader() {
        return ClassLoader.getPlatformClassLoader();
    }
}
