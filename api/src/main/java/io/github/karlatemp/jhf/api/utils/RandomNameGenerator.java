package io.github.karlatemp.jhf.api.utils;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RandomNameGenerator implements NameGenerator {
    public static final RandomNameGenerator INSTANCE = new RandomNameGenerator();
    public static final NameGenerator GENERATOR = new NonRepeatingNameGenerator(
            new MapMirroredSet<>(new ConcurrentHashMap<>()),
            RandomNameGenerator.INSTANCE,
            5
    );

    @Override
    public String getNextName(String origin) {
        return UUID.randomUUID().toString();
    }
}
