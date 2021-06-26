package io.github.karlatemp.jhf.core.mixin;

import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.IConsumer;

import java.util.Collection;
import java.util.Collections;

public class JHMPlatformServiceAgent implements IMixinPlatformServiceAgent {
    @Override
    public void init() {

    }

    @Override
    public String getSideName() {
        return "Jvm hook framework";
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return Collections.emptyList();
    }

    @Override
    public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
        if (phase == MixinEnvironment.Phase.PREINIT) {
            phaseConsumer.accept(MixinEnvironment.Phase.DEFAULT);
        }
    }

    @Override
    public void unwire() {
    }

    @Override
    public AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
        return AcceptResult.REJECTED;
    }

    @Override
    public String getPhaseProvider() {
        return "Jvm Hook Framework";
    }

    @Override
    public void prepare() {
    }

    @Override
    public void initPrimaryContainer() {
    }

    @Override
    public void inject() {
    }
}
