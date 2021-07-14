package io.github.karlatemp.jhf.core.startup;

import io.github.karlatemp.jhf.api.JvmHookFramework;
import io.github.karlatemp.jhf.api.event.EventPriority;
import io.github.karlatemp.jhf.api.events.TransformBytecodeEvent;
import io.github.karlatemp.jhf.api.markers.MarkerMirrorInitialize;
import io.github.karlatemp.jhf.api.utils.ClassFinder;
import io.github.karlatemp.jhf.core.builtin.BuiltInProcessors;
import io.github.karlatemp.jhf.core.config.JHFConfig;
import io.github.karlatemp.jhf.core.mixin.JHFBytecodeProvider;
import io.github.karlatemp.jhf.core.mixin.JHFClassProvider;
import io.github.karlatemp.jhf.core.plugin.PluginClassLoader;
import io.github.karlatemp.jhf.core.redirect.StackReMapInfo;
import io.github.karlatemp.jhf.core.utils.PlatformClassLoaders;
import io.github.karlatemp.mxlib.MxLib;
import io.github.karlatemp.mxlib.logger.MLogger;
import io.github.karlatemp.mxlib.utils.StringBuilderFormattable;
import io.github.karlatemp.unsafeaccessor.Root;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;

public class JvmHookFrameworkStartup {
    static Instrumentation instrumentation;

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        new PropertiesUtil("log4j2.StatusLogger.properties");

        ConfigurationFactory.setConfigurationFactory(new JHFCfFactory());
        MixinBootstrap.init();
        try {
            Constructor<?> cc = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer")
                    .getDeclaredConstructor();
            Root.openAccess(cc);
            cc.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    static class VMTransfer implements ClassFileTransformer {
        static final ClassLoader VMH = VMTransfer.class.getClassLoader();
        static ClassLoader PLCL;
        static ClassLoader SYS_PLATFORM_CCL;
        static Class<?> DELEGATING_CLASSLOADER_CLASS;
        MLogger logger = MxLib.getLoggerOrStd("JHF.TF");

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (loader == null) return null; // java.base
            if (loader == SYS_PLATFORM_CCL) return null; // JDK System Platform ClassLoader
            if (loader == VMH) return null;  // JvmHookFramework
            if (loader == PLCL) return null; // plugins

            if (DELEGATING_CLASSLOADER_CLASS != null && DELEGATING_CLASSLOADER_CLASS.isInstance(loader))
                return null; // java.lang.reflect.

            TransformBytecodeEvent event = new TransformBytecodeEvent();
            event.bytecode = classfileBuffer;
            event.name = className;
            event.classLoader = loader;
            event.protectionDomain = protectionDomain;

            JHFBytecodeProvider.CC_IN_TRANS = loader;
            JHFBytecodeProvider.LOADING_C = classfileBuffer;
            JHFBytecodeProvider.LOADING_N = className;

            try {
                if (JHFConfig.INSTANCE.verbose.bytecodeTransform) {
                    logger.debug(StringBuilderFormattable.by("Transforming ").plusMsg(className).plusMsg(" in classloader ").plusMsg(loader));
                }

                byte[] resp = TransformBytecodeEvent.EVENT_LINE.post(event).bytecode;

                if (resp == classfileBuffer) return null;
                return resp;
            } catch (Throwable throwable) {
                logger.warn(throwable);
                ClassWriter cw = new ClassWriter(0);
                cw.visit(Opcodes.V1_8, 0, "## Error in class transform: " + throwable, null, null, null);
                return cw.toByteArray();
            }
        }
    }

    public static void bootstrap(Instrumentation instrumentation) throws Throwable {
        MarkerMirrorInitialize.initialize();
        JHFConfig.reload();
        VMTransfer.SYS_PLATFORM_CCL = ClassFinder.findClass(
                JvmHookFrameworkStartup.class.getClassLoader(),
                "io.github.karlatemp.jhf.core.utils.PlatformClassLoaders9",
                "io.github.karlatemp.jhf.core.utils.PlatformClassLoaders"
        ).asSubclass(PlatformClassLoaders.class).getConstructor().newInstance().platformClassLoader();

        VMTransfer.DELEGATING_CLASSLOADER_CLASS = ClassFinder.findClass(null,
                "jdk.internal.reflect.DelegatingClassLoader",
                "sun.reflect.DelegatingClassLoader"
        );

        run();
        {
            Field field = JvmHookFramework.class.getDeclaredField("INSTANCE");
            Root.openAccess(field);
            field.set(null, new JHFrameworkImpl());
        }


        IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        TransformBytecodeEvent.EVENT_LINE.register(EventPriority.NORMAL, event -> {
            String name = event.name;
            if (name == null) {
                name = event.name = new ClassReader(event.bytecode).getClassName();
            }
            event.bytecode = transformer.transformClassBytes(null, name.replace('/', '.'), event.bytecode);
        });


        BuiltInProcessors.setup();
        JvmHookFrameworkStartup.instrumentation = instrumentation;

        PluginClassLoader.loadAndBootstrap(
                new File(JHFConfig.workingDir, "plugins"),
                it -> {
                    VMTransfer.PLCL = it;
                    Field ccl = JHFClassProvider.class.getDeclaredField("CCL");
                    ccl.setAccessible(true);
                    ccl.set(null, VMTransfer.PLCL);
                    return null;
                }
        );

        if (instrumentation != null) {
            StackReMapInfo.refineReflectionFactory(instrumentation);
            instrumentation.addTransformer(new VMTransfer(), true);
        }
    }
}
