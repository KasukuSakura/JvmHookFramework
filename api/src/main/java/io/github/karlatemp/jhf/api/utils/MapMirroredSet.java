package io.github.karlatemp.jhf.api.utils;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MapMirroredSet<S> extends AbstractSet<S> {
    public static final Object PLACEHOLDER = new Object();
    private final Map<S, Object> mirror;


    public MapMirroredSet(Map<S, Object> mirror) {
        this.mirror = mirror;
    }

    @Override
    public boolean add(S s) {
        return mirror.put(s, PLACEHOLDER) == null;
    }

    @Override
    public boolean remove(Object o) {
        return mirror.remove(o, PLACEHOLDER);
    }

    @Override
    public boolean contains(Object o) {
        return mirror.containsKey(o);
    }

    @Override
    public Spliterator<S> spliterator() {
        return mirror.keySet().spliterator();
    }

    @Override
    public Stream<S> stream() {
        return mirror.keySet().stream();
    }

    @Override
    public Stream<S> parallelStream() {
        return mirror.keySet().parallelStream();
    }

    @Override
    public void forEach(Consumer<? super S> action) {
        mirror.keySet().forEach(action);
    }

    @Override
    public boolean isEmpty() {
        return mirror.isEmpty();
    }

    @Override
    public void clear() {
        mirror.clear();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return mirror.keySet().toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return mirror.keySet().toArray(a);
    }

    @Override
    public Iterator<S> iterator() {
        return mirror.keySet().iterator();
    }

    @Override
    public int size() {
        return mirror.size();
    }
}
