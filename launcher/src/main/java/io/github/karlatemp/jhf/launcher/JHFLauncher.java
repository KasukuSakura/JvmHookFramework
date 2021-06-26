package io.github.karlatemp.jhf.launcher;

import sun.misc.Unsafe;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@SuppressWarnings("ConstantConditions")
public class JHFLauncher {
    static Unsafe unsafe;

    public static void launch(Instrumentation instrumentation) throws Throwable {
        {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        }

        FlattenJarFile inf;
        try (DataInputStream inp = new DataInputStream(
                new BufferedInputStream(JHFLauncher.class.getResourceAsStream("/dmp.inf"))
        )) {
            inf = FlattenJarFile.read(inp);
        }
        long pointer = unsafe.allocateMemory(inf.imageSize);
        long offset = 0;
        try (InputStream data = new BufferedInputStream(new BufferedInputStream(JHFLauncher.class.getResourceAsStream("/dmp.bin")))) {
            byte[] buffer = new byte[2048];
            while (true) {
                int ln = data.read(buffer);
                if (ln == -1) break;
                if (Unsafe.ARRAY_BYTE_INDEX_SCALE != 1) {
                    for (int i = 0; i < ln; i++) {
                        unsafe.putByte(pointer + offset + i, buffer[i]);
                    }
                } else {
                    unsafe.copyMemory(buffer, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, pointer + offset, ln);
                }
                offset += ln;
            }
        }

        /*for (FlattenJarFile.ResPair resPair : inf.resources) {
            System.out.println(resPair.name);
        }*/

        OmitCCL ccl = new OmitCCL(inf, pointer);
        try {
            Class.forName("io.github.karlatemp.jhf.launcher.OmitMod")
                    .getDeclaredMethod("omit", OmitCCL.class, FlattenJarFile.class, Instrumentation.class)
                    .invoke(null, ccl, inf, instrumentation);
        } catch (ClassFormatError ignore) {
        }
        pkgSetup(ccl, inf);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ccl);
        ccl.loadClass("io.github.karlatemp.jhf.core.startup.JvmHookFrameworkStartup")
                .getMethod("bootstrap", Instrumentation.class)
                .invoke(null, instrumentation);
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    private static void pkgSetup(OmitCCL ccl, FlattenJarFile inf) throws Exception {
        for (FlattenJarFile.ModuleInfo mod : inf.modules) {
            Manifest manifest = new Manifest();
            if (mod.manifest != null) {
                manifest.read(new ByteArrayInputStream(mod.manifest));
            }
            Attributes mainAttributes = manifest.getMainAttributes();
            String specTitle = mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
            String specVersion = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
            String specVendor = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
            String implTitle = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
            String implVersion = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            String implVendor = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
            for (String pkg : mod.packages) {
                ccl.definePackage0(
                        pkg.replace('/', '.'),
                        specTitle, specVersion, specVendor,
                        implTitle, implVersion, implVendor,
                        null
                );
            }
        }
    }
}
