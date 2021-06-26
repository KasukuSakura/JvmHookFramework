package io.github.karlatemp.jhf.api.events;

import io.github.karlatemp.jhf.api.JvmHookFramework;
import io.github.karlatemp.jhf.api.event.EventHandler;
import io.github.karlatemp.jhf.api.event.EventLine;
import io.github.karlatemp.jhf.api.utils.SneakyThrow;

import java.lang.reflect.Member;

public class JavaLangReflectInvokeEvent {
    public static final EventLine<JavaLangReflectInvokeEvent> EVENT_LINE = new EventLine<JavaLangReflectInvokeEvent>() {
        @SuppressWarnings("rawtypes")
        @Override
        protected void onHandleException(EventHandler handler, JavaLangReflectInvokeEvent event, Exception exception) {
            JvmHookFramework.getInstance().hiddenStackTrack(exception);
            SneakyThrow.throw0(exception);
        }
    };

    public enum Type {
        INVOKE_METHOD,
        INVOKE_CONSTRUCTOR,
        GET_FIELD,
        SET_FIELD
    }

    public final Type type;
    public final Member target;
    public final Object thiz;
    public final Class<?> caller;
    public final Object[] args; // exists only when invoking Method/Constructor

    public JavaLangReflectInvokeEvent(
            Type type,
            Member target, Object thiz, Class<?> caller, Object[] args
    ) {
        this.type = type;
        this.target = target;
        this.thiz = thiz;
        this.caller = caller;
        this.args = args;
    }
}
