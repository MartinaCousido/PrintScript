package org.printscript.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.printscript.config.ConfigurationReader;
import org.printscript.config.JsonConfigurationMapper;
import org.printscript.config.RulesConfiguration;
import org.printscript.config.YamlConfigurationMapper;

// Lee el archivo de reglas del usuario (JSON/YAML, formato detectado por extensión)
// hacia una RulesConfiguration genérica. Lo comparten las operaciones que se
// configuran con reglas: analyzing (linter) y formatting (formatter). cli es el único
// módulo autorizado a abrir archivos (docs/arquitectura.md, sección 5).
final class ConfigurationFiles {

    private ConfigurationFiles() {}

    // configFile null: configuración vacía, así cada regla cae en su valor por defecto
    static RulesConfiguration read(Path configFile) throws IOException {
        if (configFile == null) {
            return new RulesConfiguration(Map.of());
        }
        if (!Files.isRegularFile(configFile)) {
            throw new IllegalArgumentException(
                    "el archivo de configuración '" + configFile + "' no existe");
        }
        String format = formatOf(configFile);
        try (InputStream source = Files.newInputStream(configFile)) {
            ConfigurationReader reader =
                    new ConfigurationReader(
                            List.of(new JsonConfigurationMapper(), new YamlConfigurationMapper()));
            return reader.read(source, format);
        }
    }

    private static String formatOf(Path configFile) {
        String name = configFile.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException(
                    "el archivo de configuración no tiene extensión (.json/.yaml/.yml): "
                            + configFile);
        }
        return name.substring(dot + 1);
    }
}
