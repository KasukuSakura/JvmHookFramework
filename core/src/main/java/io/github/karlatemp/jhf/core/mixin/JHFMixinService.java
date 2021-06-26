package io.github.karlatemp.jhf.core.mixin;

import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.*;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public class JHFMixinService extends MixinServiceAbstract {
    private final JHFClassProvider classProvider = new JHFClassProvider();
    private final JHFBytecodeProvider bytecodeProvider = new JHFBytecodeProvider();
    private final IContainerHandle containerHandle = new ContainerHandleVirtual("Jvm Hook Framework");

    @Override
    public String getName() {
        return "JVM Hook Framework";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return classProvider;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return bytecodeProvider;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return null;
    }

    @Override
    public IClassTracker getClassTracker() {
        return null;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.singleton(JHMPlatformServiceAgent.class.getName());
    }


    @Override
    public IContainerHandle getPrimaryContainer() {
        return containerHandle;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return JHFMixinService.class.getResourceAsStream("/" + name);
    }

    @Override
    public void prepare() {
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_11;
    }
}
