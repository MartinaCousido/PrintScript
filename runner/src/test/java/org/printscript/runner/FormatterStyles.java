package org.printscript.runner;

import java.util.Optional;
import org.printscript.formatter.config.FormatterConfiguration;

// Los estilos que usan los tests de punta a punta, nombrados una sola vez.
//
// El formatter solo toca lo que alguna regla pide, así que "el estilo de la consigna" es
// la suma de las reglas que lo componen y no un valor por defecto escondido.
final class FormatterStyles {

    private FormatterStyles() {}

    static FormatterConfiguration consigna() {
        return new FormatterConfiguration(
                Optional.of(true), // espacio alrededor del '='
                false, // sin espacio antes del ':'
                true, // espacio después del ':'
                true, // espacio alrededor de + - * /
                false,
                true, // una sentencia por línea
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    // la regla mínima: solo el espacio alrededor del '='
    static FormatterConfiguration soloElEspacioDelIgual() {
        return new FormatterConfiguration(
                Optional.of(true),
                false,
                false,
                false,
                false,
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    static FormatterConfiguration conSaltosDespuesDelPrintln(int howMany) {
        FormatterConfiguration base = consigna();
        return new FormatterConfiguration(
                base.spacingAroundEquals(),
                base.spacingBeforeColon(),
                base.spacingAfterColon(),
                base.spaceSurroundingOperations(),
                base.singleSpaceSeparation(),
                base.lineBreakAfterStatement(),
                Optional.of(howMany),
                Optional.empty(),
                Optional.empty());
    }

    static FormatterConfiguration consignaConBloques(int indent, boolean braceOnSameLine) {
        FormatterConfiguration base = consigna();
        return new FormatterConfiguration(
                base.spacingAroundEquals(),
                base.spacingBeforeColon(),
                base.spacingAfterColon(),
                base.spaceSurroundingOperations(),
                base.singleSpaceSeparation(),
                base.lineBreakAfterStatement(),
                base.lineBreaksAfterPrintln(),
                Optional.of(braceOnSameLine),
                Optional.of(indent));
    }
}
