package org.printscript.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

// recibe una lista de mappers, elige el primero que soporta el formato y delega
public final class ConfigurationReader {

    private final List<ConfigurationMapper> mappers;

    public ConfigurationReader(List<ConfigurationMapper> mappers) {
        this.mappers = List.copyOf(mappers);
        if (this.mappers.isEmpty()) {
            throw new IllegalArgumentException("At least one configuration mapper is required");
        }
    }

    public static ConfigurationReader standard() {
        return new ConfigurationReader(
                List.of(new JsonConfigurationMapper(), new YamlConfigurationMapper()));
    }

    public RulesConfiguration read(InputStream source, String format) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(format, "format");
        String normalized = format.toLowerCase(Locale.ROOT);
        ConfigurationMapper mapper =
                mappers.stream()
                        .filter(candidate -> candidate.supports(normalized))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Formato de configuración no soportado: "
                                                        + format));
        Map<String, Object> values = readWith(mapper, source, format);
        if (values == null || values.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "La configuración debe ser un objeto con valores no nulos");
        }
        return new RulesConfiguration(values);
    }

    private static Map<String, Object> readWith(
            ConfigurationMapper mapper, InputStream source, String format) {
        try {
            return mapper.read(source);
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Configuración " + format + " inválida: " + exception.getMessage(), exception);
        }
    }
}
