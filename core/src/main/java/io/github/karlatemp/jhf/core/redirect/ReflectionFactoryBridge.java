package io.github.karlatemp.jhf.core.redirect;

import io.github.karlatemp.jhf.api.events.ReflectionAccessorGenerateEvent;
import io.github.karlatemp.jhf.api.events.ReflectionAccessorPreGenerateEvent;

import java.lang.reflect.Member;

public class ReflectionFactoryBridge {
    public static Object preRequest(Object member) {
        ReflectionAccessorGenerateEvent event = new ReflectionAccessorGenerateEvent();
        event.requested = (Member) member;
        ReflectionAccessorGenerateEvent.EVENT_LINE.post(event);
        return event.response;
    }

    public static Object remap(Object member) {
        ReflectionAccessorPreGenerateEvent event = new ReflectionAccessorPreGenerateEvent();
        event.member = (Member) member;
        ReflectionAccessorPreGenerateEvent.EVENT_LINE.post(event);
        return event.member;
    }
}
