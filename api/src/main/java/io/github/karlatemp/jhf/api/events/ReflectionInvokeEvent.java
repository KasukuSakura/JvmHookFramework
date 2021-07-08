package io.github.karlatemp.jhf.api.events;

import io.github.karlatemp.jhf.api.event.EventHandler;
import io.github.karlatemp.jhf.api.event.EventLine;
import io.github.karlatemp.jhf.api.event.EventPriority;
import io.github.karlatemp.jhf.api.utils.FilterMembers;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ReflectionInvokeEvent {
    public static final EventLine<ReflectionInvokeEvent> EVENT_LINE = new EventLine.DirectThrowException<>();

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

    static {
        EVENT_LINE.register(EventPriority.LOWEST, new EventHandler<ReflectionInvokeEvent>() {
            private boolean filteted(Member member) {
                for (Predicate<Member> filter : FilterMembers.filters) {
                    if (filter.test(member)) return true;
                }
                return false;
            }

            @Override
            public void handle(ReflectionInvokeEvent event) throws Exception {
                switch (event.type) {
                    case GetField:
                    case GetDeclaredField:
                    case GetConstructor:
                    case GetDeclaredConstructor:
                    case GetMethod:
                    case GetDeclaredMethod: {
                        Member m = (Member) event.resp;
                        if (filteted(m)) {
                            if (m instanceof Field) {
                                throw new NoSuchFieldException(m.getName());
                            }
                            throw new NoSuchMethodException(m.getName());
                        }
                        break;
                    }
                    case GetFields:
                    case GetDeclaredFields:
                    case GetConstructors:
                    case GetDeclaredConstructors:
                    case GetMethods:
                    case GetDeclaredMethods: {
                        event.resp = Stream.of((Member[]) event.resp)
                                .filter(it -> !filteted(it))
                                .toArray(size -> (Object[]) Array.newInstance(event.resp.getClass().getComponentType(), size));
                        break;
                    }
                }
            }
        });
    }
}
