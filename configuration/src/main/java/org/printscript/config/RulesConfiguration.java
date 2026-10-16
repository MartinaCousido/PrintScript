package org.printscript.config;

import java.util.Map;
import java.util.Optional;

// el resultado
// guarda lo que decía cada archivo
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

    public Optional<Integer> getInt(String key) {
        return valueOf(key, Integer.class);
    }

    /** Validates recognized keys without rejecting keys belonging to other rule sets. */
    public void validateTypes(Map<String, Class<?>> schema) {
        schema.forEach(
                (key, type) -> {
                    Object value = values.get(key);
                    if (value != null && !type.isInstance(value)) {
                        throw new IllegalArgumentException(
                                "La regla '" + key + "' requiere " + type.getSimpleName());
                    }
                });
    }

    private <T> Optional<T> valueOf(String key, Class<T> type) {
        return Optional.ofNullable(values.get(key)).filter(type::isInstance).map(type::cast);
    }
}
