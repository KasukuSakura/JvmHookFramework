package io.github.karlatemp.jhf.api.events;

import io.github.karlatemp.jhf.api.event.EventLine;

import java.security.ProtectionDomain;

public class TransformBytecodeEvent {
    public static final EventLine<TransformBytecodeEvent> EVENT_LINE = new EventLine.DirectThrowException<>();

    public byte[] bytecode;
    public String name;
    public ClassLoader classLoader;
    public ProtectionDomain protectionDomain;
}
