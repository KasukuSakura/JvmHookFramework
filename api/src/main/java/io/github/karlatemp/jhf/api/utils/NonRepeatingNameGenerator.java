package io.github.karlatemp.jhf.api.utils;

import java.util.Set;

public class NonRepeatingNameGenerator implements NameGenerator {
    private final Set<String> cache;
    private final NameGenerator nameGenerator;
    private final int failedLimit;

    public NonRepeatingNameGenerator(Set<String> cache, NameGenerator nameGenerator, int failedLimit) {
        this.cache = cache;
        this.nameGenerator = nameGenerator;
        this.failedLimit = failedLimit;
    }

    @Override
    public String getNextName(String origin) {
        String resp = this.nameGenerator.getNextName(origin);
        if (cache.add(resp)) return resp;
        int limit = failedLimit;
        while (limit-- > 0) {
            resp = this.nameGenerator.getNextName(origin);
            if (cache.add(resp)) return resp;
        }
        while (true) {
            resp = RandomNameGenerator.INSTANCE.getNextName(origin);
            if (cache.add(resp))
                return resp;
        }
    }
}
