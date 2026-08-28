package org.printscript.config;

import java.util.Map;
import java.util.Optional;

public final class RulesConfiguration {

    private final Map<String, Object> values;

    public RulesConfiguration(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    public Optional<String> getString(String key) {
        return valueOf(key, String.class);
    }

    public Optional<Boolean> getBoolean(String key) {
        return valueOf(key, Boolean.class);
    }

    private <T> Optional<T> valueOf(String key, Class<T> type) {
        return Optional.ofNullable(values.get(key)).filter(type::isInstance).map(type::cast);
    }
}
