package io.github.karlatemp.jhf.api;

import io.github.karlatemp.jhf.api.event.EventHandler;
import io.github.karlatemp.jhf.api.event.EventLine;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.io.File;
import java.nio.file.Path;

public abstract class JvmHookFramework {
    private static JvmHookFramework INSTANCE;

    public static JvmHookFramework getInstance() {
        return INSTANCE;
    }

    public abstract void hiddenStackTrack(Throwable throwable);

    public abstract <T> void onEventException(
            EventLine<T> eventLine,
            EventHandler<? super T> handler,
            T event,
            Exception exception
    );

    public abstract File getWorkingDir();

    public File getDataFolder(String name) {
        return new File(getWorkingDir(), "data/" + name);
    }

    public abstract HoconConfigurationLoader newConfigLoader(ConfigurationOptions options, Path path);

    public abstract TypeSerializerCollection jhfExtractTypeSerializers();
}
