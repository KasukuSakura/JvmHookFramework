package io.github.karlatemp.jhf.launcher;

import sun.misc.Unsafe;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;

class OmitCCL extends ClassLoader {
    private final long image;
    private final FlattenJarFile info;
    private final PermissionCollection pcc;
    private final URL base;
    protected Function<String, Class<?>> CUSTOM_C_FIND; // setup by reflection in core

    /*
    static final MethodHandle getDefinedPackage_handle;

    static {
        try {
            MethodHandles.Lookup lk = MethodHandles.lookup();
            MethodHandle h;
            try {
                h = lk.findVirtual(ClassLoader.class, "getDefinedPackage", MethodType.methodType(Package.class, String.class));
            } catch (NoSuchMethodException e) {
                h = lk.findVirtual(ClassLoader.class, "getPackage", MethodType.methodType(Package.class, String.class));
            }
            getDefinedPackage_handle = h;
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }
     */

    protected Package definePackage0(String name, String specTitle,
                                     String specVersion, String specVendor,
                                     String implTitle, String implVersion,
                                     String implVendor, URL sealBase) {
        return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }

    OmitCCL(FlattenJarFile info, long image) throws MalformedURLException {
        super(OmitCCL.class.getClassLoader());
        this.info = info;
        this.image = image;
        AllPermission ap = new AllPermission();
        PermissionCollection collection = ap.newPermissionCollection();
        pcc = collection;
        collection.add(ap);
        collection.setReadOnly();
        base = new URL(
                "jhfnative",
                "localhost",
                0,
                "/",
                new NativeUHandler(image, info.imageSize)
        );
    }

    @Override
    protected URL findResource(String name) {
        for (FlattenJarFile.ResPair s : info.resources) {
            if (s.name.equals(name)) {
                try {
                    return new URL(base, "/" + s.pointer + "-" + s.size);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        List<URL> resp = new ArrayList<>();
        for (FlattenJarFile.ResPair s : info.resources) {
            if (s.name.equals(name)) {
                try {
                    resp.add(new URL(base, "/" + s.pointer + "-" + s.size));
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return new ItrEnumeration<>(resp.iterator());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (CUSTOM_C_FIND != null) {
            Class<?> rsp = CUSTOM_C_FIND.apply(name);
            if (rsp != null) return rsp;
        }
        String rsName = name.replace('.', '/') + ".class";
        for (FlattenJarFile.ResPair s : info.resources) {
            if (s.name.equals(rsName)) {
                ProtectionDomain domain = new ProtectionDomain(
                        new CodeSource(null, resolveCerts(s.signers)), pcc
                );
                return defineClass(name, buffer(s), domain);
            }
        }
        throw new ClassNotFoundException(name);
    }

    private ByteBuffer buffer(FlattenJarFile.ResPair s) {
        byte[] snapshot = new byte[(int) s.size];
        if (Unsafe.ARRAY_BYTE_INDEX_SCALE == 1) {
            JHFLauncher.unsafe.copyMemory(null, image + s.pointer, snapshot, Unsafe.ARRAY_BYTE_BASE_OFFSET, s.size);
        } else {
            long basePointer = image + s.pointer;
            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i] = JHFLauncher.unsafe.getByte(basePointer);
                basePointer++;
            }
        }
        return ByteBuffer.wrap(snapshot);
    }

    private Certificate[] resolveCerts(int[] signers) {
        if (signers.length == 0) return null;
        Certificate[] certificates = new Certificate[signers.length];
        for (int i = 0; i < signers.length; i++) {
            certificates[i] = this.info.certificates.get(i);
        }
        return certificates;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        for (FlattenJarFile.ResPair s : info.resources) {
            if (s.name.equals(name)) {
                return new NativeImageInputStream(this.image + s.pointer, s.size);
            }
        }
        return null;
    }
}
