package io.github.karlatemp.jhf.api.event;

public interface EventHandler<T> {
    public void handle(T event) throws Exception;
}
