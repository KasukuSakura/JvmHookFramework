package io.github.karlatemp.jhf.api.event;

import io.github.karlatemp.jhf.api.JvmHookFramework;
import io.github.karlatemp.jhf.api.utils.SneakyThrow;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public class EventLine<T> {
    protected final Map<EventPriority, Collection<EventHandler<? super T>>> handlers = new EnumMap<>(EventPriority.class);

    public T post(T event) {
        for (EventPriority p : EventPriority.values0) {
            Collection<EventHandler<? super T>> handlers = this.handlers.get(p);
            if (handlers == null) continue;
            for (EventHandler<? super T> handler : handlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    onHandleException(handler, event, e);
                }
            }
        }
        return event;
    }

    public void register(EventPriority priority, EventHandler<? super T> handler) {
        handlers.computeIfAbsent(priority, $ -> new ConcurrentLinkedDeque<>())
                .add(handler);
    }

    public void unregister(EventHandler<? super T> handler) {
        for (Collection<EventHandler<? super T>> handlers : this.handlers.values()) {
            handlers.remove(handler);
        }
    }

    protected void onHandleException(EventHandler<? super T> handler, T event, Exception exception) {
        JvmHookFramework.getInstance().onEventException(this, handler, event, exception);
    }

    public static class DirectThrowException<T> extends EventLine<T> {
        @Override
        protected void onHandleException(EventHandler<? super T> handler, T event, Exception exception) {
            JvmHookFramework.getInstance().hiddenStackTrack(exception);
            SneakyThrow.throw0(exception);
        }
    }
}
