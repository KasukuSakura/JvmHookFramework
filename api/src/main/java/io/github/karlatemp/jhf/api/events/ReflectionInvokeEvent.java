package io.github.karlatemp.jhf.api.events;

import io.github.karlatemp.jhf.api.JvmHookFramework;
import io.github.karlatemp.jhf.api.event.EventHandler;
import io.github.karlatemp.jhf.api.event.EventLine;
import io.github.karlatemp.jhf.api.utils.SneakyThrow;

public class ReflectionInvokeEvent {
    public static final EventLine<ReflectionInvokeEvent> EVENT_LINE = new EventLine<ReflectionInvokeEvent>() {
        @Override
        protected void onHandleException(
                EventHandler<? super ReflectionInvokeEvent> handler,
                ReflectionInvokeEvent event,
                Exception exception
        ) {
            JvmHookFramework.getInstance().hiddenStackTrack(exception);
            SneakyThrow.throw0(exception);
        }
    };

    public Type type;
    public Object resp;

    public ReflectionInvokeEvent(Type type, Object resp) {
        this.type = type;
        this.resp = resp;
    }

    public enum Type {
        GetMethods,
        GetDeclaredMethods,
        GetMethod,
        GetDeclaredMethod,
        GetFields,
        GetDeclaredFields,
        GetField,
        GetDeclaredField,
        GetConstructor,
        GetDeclaredConstructor,
        GetConstructors,
        GetDeclaredConstructors,
    }
}
