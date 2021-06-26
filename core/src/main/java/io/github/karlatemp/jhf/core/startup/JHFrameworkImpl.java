package io.github.karlatemp.jhf.core.startup;

import io.github.karlatemp.jhf.api.JvmHookFramework;
import io.github.karlatemp.jhf.api.event.EventHandler;
import io.github.karlatemp.jhf.api.event.EventLine;
import io.github.karlatemp.jhf.core.utils.HiddenStackTrack;
import io.github.karlatemp.mxlib.MxLib;
import io.github.karlatemp.mxlib.logger.MLogger;
import io.github.karlatemp.mxlib.utils.StringBuilderFormattable;

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
}
