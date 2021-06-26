package io.github.karlatemp.jhf.api;

import io.github.karlatemp.jhf.api.event.EventHandler;
import io.github.karlatemp.jhf.api.event.EventLine;

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
}
