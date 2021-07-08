package io.github.karlatemp.jhf.core.redirect;

import io.github.karlatemp.jhf.api.events.ReflectionAccessorGenerateEvent;

import java.lang.reflect.Member;

public class ReflectionFactoryBridge {
    public static Object preRequest(Object member) {
        ReflectionAccessorGenerateEvent event = new ReflectionAccessorGenerateEvent();
        event.requested = (Member) member;
        ReflectionAccessorGenerateEvent.EVENT_LINE.post(event);
        return event.response;
    }
}
