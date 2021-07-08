package io.github.karlatemp.jhf.api.events;

import io.github.karlatemp.jhf.api.event.EventLine;

import java.lang.reflect.Member;

public class ReflectionAccessorGenerateEvent {
    public static final EventLine<ReflectionAccessorGenerateEvent> EVENT_LINE = new EventLine<>();
    public Object response; // One of FieldAccessor, MethodAccessor, ConstructorAccessor
    public Member requested;
}
