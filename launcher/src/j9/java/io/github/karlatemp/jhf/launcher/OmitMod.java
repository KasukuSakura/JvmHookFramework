package io.github.karlatemp.jhf.launcher;

import java.io.ByteArrayInputStream;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

class OmitMod {
    static boolean disabled = true;

    static Class<?> findC(String... names) throws ClassNotFoundException {
        for (String s : names) {
            try {
                return Class.forName(s, false, null);
            } catch (ClassNotFoundException ignore) {
            }
        }
        throw new ClassNotFoundException("Cannot find any of " + String.join(", ", names));
    }

    static void omit(OmitCCL ccl, FlattenJarFile info, Instrumentation instrumentation) throws Throwable {
        if (disabled) return; // Modules are really hard to use
        if (instrumentation == null) return;

        Module cmod = OmitMod.class.getModule();
        Class<?> SharedSecrets = findC(
                "jdk.internal.access.SharedSecrets",
                "jdk.internal.misc.SharedSecrets"
        );
        instrumentation.redefineModule(
                Object.class.getModule(), Set.of(), Map.of(),
                Map.of(
                        "java.lang", Set.of(cmod),
                        "java.lang.module", Set.of(cmod),
                        SharedSecrets.getPackageName(), Set.of(cmod)
                ),
                Set.of(), Map.of()
        );
        MethodHandles.Lookup lk = MethodHandles.lookup();
        MethodHandle newBuilder = MethodHandles.privateLookupIn(ModuleDescriptor.Builder.class, lk)
                .findConstructor(ModuleDescriptor.Builder.class, MethodType.methodType(void.class, String.class, boolean.class, Set.class));
        MethodHandle defineModule = MethodHandles.privateLookupIn(Module.class, lk)
                .findConstructor(Module.class, MethodType.methodType(void.class, ModuleLayer.class, ClassLoader.class, ModuleDescriptor.class, URI.class));
        Module EVERYONE_MODULE = (Module) MethodHandles.privateLookupIn(Module.class, lk)
                .findStaticGetter(Module.class, "EVERYONE_MODULE", Module.class)
                .invoke();

        Class<?> JLAC = Class.forName(SharedSecrets.getPackageName() + ".JavaLangAccess", false, null);

        /*
        void addReads(Module m1, Module m2);

        Object jla = lk.findStatic(SharedSecrets, "getJavaLangAccess", MethodType.methodType(JLAC)).invoke();
        MethodHandle addReads = lk.findVirtual(JLAC, "addReads", MethodType.methodType(void.class, Module.class, Module.class));
        MethodHandle addOpens = lk.findVirtual(JLAC, "addExports", MethodType.methodType(void.class, Module.class, String.class, Module.class));
        MethodHandle addExports = lk.findVirtual(JLAC, "addOpens", MethodType.methodType(void.class, Module.class, String.class, Module.class));
        */

        List<Module> modules = new ArrayList<>();
        for (FlattenJarFile.ModuleInfo minfo : info.modules) {
            ModuleDescriptor.Builder builder;
            builder = (ModuleDescriptor.Builder) newBuilder.invoke(minfo.name, false, Set.of());
            builder.packages(new HashSet<>(minfo.packages));
            Manifest manifest = new Manifest(new ByteArrayInputStream(minfo.manifest));
            builder.version(manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION));
            for (String pkg : minfo.packages) {
                builder.opens(pkg.replace('/', '.'));
                builder.exports(pkg.replace('/', '.'));
            }
            builder.requires("java.base");
            builder.requires("java.management");

            ModuleDescriptor descriptor = builder.build();
            modules.add((Module) defineModule.invoke(null, ccl, descriptor, null));
        }

        Set<Module> EVERYONE_MODULE_SET = Set.of(EVERYONE_MODULE);
        for (Module m : modules) {
            Set<Module> extraReads = new HashSet<>();
            extraReads.add(Object.class.getModule());
            extraReads.addAll(modules);
            extraReads.remove(m);

            Map<String, Set<Module>> extraOpens = new HashMap<>();

            for (String pkg : m.getDescriptor().packages()) {
                extraOpens.put(pkg, EVERYONE_MODULE_SET);
            }

            instrumentation.redefineModule(
                    m,
                    extraReads,
                    extraOpens,
                    extraOpens, Set.of(), Map.of()
            );
        }

    }
}
