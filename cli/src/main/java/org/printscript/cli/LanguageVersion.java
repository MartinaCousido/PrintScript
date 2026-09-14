package org.printscript.cli;

import org.printscript.common.Version;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

// la versión del lenguaje tal como se escribe en la línea de comandos
public enum LanguageVersion {
    V1_0("1.0", Version.V10),
    V1_1("1.1", Version.V11);

    private final String written;
    private final Version language;

    LanguageVersion(String written, Version language) {
        this.written = written;
        this.language = language;
    }

    String label() {
        return written;
    }

    Version domain() {
        return language;
    }

    static String supported() {
        StringBuilder labels = new StringBuilder();
        for (LanguageVersion version : values()) {
            if (!labels.isEmpty()) {
                labels.append(", ");
            }
            labels.append(version.written);
        }
        return labels.toString();
    }

    static LanguageVersion of(String value) {
        for (LanguageVersion version : values()) {
            if (version.written.equals(value)) {
                return version;
            }
        }
        return null;
    }

    static final class Converter implements ITypeConverter<LanguageVersion> {

        @Override
        public LanguageVersion convert(String value) {
            LanguageVersion version = of(value);
            if (version == null) {
                throw new TypeConversionException(
                        "'"
                                + value
                                + "' no es una versión soportada (valores válidos: "
                                + supported()
                                + ")");
            }
            return version;
        }
    }
}
