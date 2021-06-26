package io.github.karlatemp.jhf.core.startup;

import io.github.karlatemp.jhf.api.JvmHookFramework;
import io.github.karlatemp.jhf.api.event.EventPriority;
import io.github.karlatemp.jhf.api.events.JavaLangReflectInvokeEvent;
import io.github.karlatemp.jhf.api.events.TransformBytecodeEvent;
import io.github.karlatemp.jhf.core.builtin.BuiltInProcessors;
import io.github.karlatemp.jhf.core.mixin.JHFClassProvider;
import io.github.karlatemp.jhf.core.plugin.PluginClassLoader;
import io.github.karlatemp.unsafeaccessor.Root;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.util.PropertiesUtil;
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
        static IMixinTransformer transformer;

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (loader == null) return null; // java.base
            if (loader == VMH) return null;  // JvmHookFramework
            if (loader == PLCL) return null; // plugins
            TransformBytecodeEvent event = new TransformBytecodeEvent();
            event.bytecode = classfileBuffer;
            event.name = className;
            event.classLoader = loader;
            event.protectionDomain = protectionDomain;

            byte[] resp = TransformBytecodeEvent.EVENT_LINE.post(event).bytecode;

            if (resp == classfileBuffer) return null;
            return resp;
        }
    }

    public static void bootstrap(Instrumentation instrumentation) throws Throwable {
        File workingDir = new File(".jvm-hook-framework");
        run();
        VMTransfer.PLCL = PluginClassLoader.loadAndBootstrap(new File(workingDir, "plugins"));
        Field ccl = JHFClassProvider.class.getDeclaredField("CCL");
        ccl.setAccessible(true);
        ccl.set(null, VMTransfer.PLCL);

        {
            Field field = JvmHookFramework.class.getDeclaredField("INSTANCE");
            Root.openAccess(field);
            field.set(null, new JHFrameworkImpl());
        }


        IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        TransformBytecodeEvent.EVENT_LINE.register(EventPriority.NORMAL, event -> {
            event.bytecode = transformer.transformClassBytes(null, event.name, event.bytecode);
        });

        JavaLangReflectInvokeEvent.EVENT_LINE.register(EventPriority.NORMAL, event -> {
            System.out.println("On Execute: " + event.type + " - " + event.target + " with caller: " + event.caller);
        });

        BuiltInProcessors.setup();

        if (instrumentation != null) {
            instrumentation.addTransformer(new VMTransfer(), true);
        }
    }
}
