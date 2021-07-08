package io.github.karlatemp.jhf.api.events;

import io.github.karlatemp.jhf.api.event.EventLine;

import java.lang.reflect.Member;

public class ReflectionAccessorPreGenerateEvent {
    public static final EventLine<ReflectionAccessorPreGenerateEvent> EVENT_LINE = new EventLine.DirectThrowException<>();
    public Member member;
}
