package io.github.karlatemp.jhf.core.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unchecked")
public class JHFGlobalPropertyService implements IGlobalPropertyService {
    private static class PropertyKey implements IPropertyKey {
        final String name;
        Object value;
        boolean setup;

        private PropertyKey(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "PropertyKey[name='" + name + "', value=" + value + "]";
        }
    }

    private final ConcurrentMap<String, PropertyKey> properties = new ConcurrentHashMap<>();

    @Override
    public IPropertyKey resolveKey(String name) {
        return properties.computeIfAbsent(name, PropertyKey::new);
    }

    @Override
    public <T> T getProperty(IPropertyKey key) {
        return (T) ((PropertyKey) key).value;
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        PropertyKey pk = (PropertyKey) key;
        pk.value = value;
        pk.setup = true;
    }

    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        PropertyKey pk = (PropertyKey) key;
        if (pk.setup) return (T) pk.value;
        return defaultValue;
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        return String.valueOf(this.<Object>getProperty(key, defaultValue));
    }
}
