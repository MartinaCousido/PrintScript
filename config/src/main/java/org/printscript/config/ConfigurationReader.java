package org.printscript.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConfigurationReader {

    private final List<ConfigurationMapper> mappers;

    public ConfigurationReader(List<ConfigurationMapper> mappers) {
        this.mappers = List.copyOf(mappers);
    }

    public RulesConfiguration read(InputStream source, String format) {
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
        return new RulesConfiguration(readWith(mapper, source, format));
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
