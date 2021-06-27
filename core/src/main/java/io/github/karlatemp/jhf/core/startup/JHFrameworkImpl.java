package io.github.karlatemp.jhf.core.startup;

import io.github.karlatemp.jhf.api.JvmHookFramework;
import io.github.karlatemp.jhf.api.event.EventHandler;
import io.github.karlatemp.jhf.api.event.EventLine;
import io.github.karlatemp.jhf.core.config.JHFConfig;
import io.github.karlatemp.jhf.core.utils.HiddenStackTrack;
import io.github.karlatemp.mxlib.MxLib;
import io.github.karlatemp.mxlib.logger.MLogger;
import io.github.karlatemp.mxlib.utils.StringBuilderFormattable;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;

class JHFrameworkImpl extends JvmHookFramework {
    private static final MLogger EVENT_LOGGER = MxLib.getLoggerFactory().getLogger("JHF.Event");

    @Override
    public void hiddenStackTrack(Throwable throwable) {
        HiddenStackTrack.hidden(throwable);
    }

    @Override
    public <T> void onEventException(
            EventLine<T> eventLine,
            EventHandler<? super T> handler,
            T event,
            Exception exception
    ) {
        EVENT_LOGGER.warn(StringBuilderFormattable.by("Exception in event pipeline [")
                        .plusMsg(event)
                        .plusMsg("]"),
                exception
        );
    }

    @Override
    public File getWorkingDir() {
        return JHFConfig.workingDir;
    }

    @Override
    public HoconConfigurationLoader newConfigLoader(ConfigurationOptions options, Path path) {
        return JHFConfig.newLoader(options, path);
    }

    @Override
    public TypeSerializerCollection jhfExtractTypeSerializers() {
        return JHFConfig.EXTRACT_SERIALIZERS;
    }

    @Override
    public Instrumentation getInstrumentation() {
        return JvmHookFrameworkStartup.instrumentation;
    }
}
