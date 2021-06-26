package io.github.karlatemp.jhf.api.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

public class FilterMembers {
    public static final ConcurrentLinkedDeque<Predicate<Member>> filters = new ConcurrentLinkedDeque<>();

    public static void registerFieldFilter(Predicate<Field> filter) {
        filters.add(it -> it instanceof Field && filter.test((Field) it));
    }

    public static void registerMethodFilter(Predicate<Method> filter) {
        filters.add(it -> it instanceof Method && filter.test((Method) it));
    }

    public static void registerConstructorFilter(Predicate<Constructor<?>> filter) {
        filters.add(it -> it instanceof Constructor && filter.test((Constructor<?>) it));
    }
}
