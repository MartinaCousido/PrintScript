package org.printscript.cli;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

// la versión del lenguaje que la consigna pide como argumento; hoy es un choice de
// una sola opción a propósito, ver docs/arquitectura.md seccion 6 (el runner no
// tiene parámetro de versión, se removió por no hacer falta todavía)
public enum LanguageVersion {
    V1_0("1.0");

    private final String label;

    LanguageVersion(String label) {
        this.label = label;
    }

    static final class Converter implements ITypeConverter<LanguageVersion> {

        @Override
        public LanguageVersion convert(String value) {
            for (LanguageVersion version : values()) {
                if (version.label.equals(value)) {
                    return version;
                }
            }
            throw new TypeConversionException(
                    "'" + value + "' no es una versión soportada (valores válidos: 1.0)");
        }
    }
}
