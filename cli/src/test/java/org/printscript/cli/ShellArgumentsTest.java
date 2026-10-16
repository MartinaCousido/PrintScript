package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ShellArgumentsTest {
    private static final String SOURCE = "mi fuente.ps";

    @Test
    void separatesQuotedSourceAndConfigPaths() {
        assertEquals(
                List.of(SOURCE, "mis reglas.yaml"),
                ShellArguments.tokenize("\"mi fuente.ps\"\t'mis reglas.yaml'"));
    }

    @Test
    void supportsEscapedQuotesAndBackslashes() {
        assertEquals(List.of("a\"b", "c\\d"), ShellArguments.tokenize("\"a\\\"b\" \"c\\\\d\""));
    }

    @Test
    void preservesLegacyUnquotedSinglePaths() {
        assertEquals(SOURCE, ShellArguments.singlePath(SOURCE));
        assertEquals(SOURCE, ShellArguments.singlePath("\"mi fuente.ps\""));
    }

    @Test
    void rejectsUnclosedQuotesAndExtraSinglePathArguments() {
        assertThrows(IllegalArgumentException.class, () -> ShellArguments.tokenize("\"unfinished"));
        assertThrows(
                IllegalArgumentException.class,
                () -> ShellArguments.singlePath("\"source\" extra"));
    }

    @Test
    void preservesEmptyQuotedArgumentsAndPlainBackslashes() {
        assertEquals(List.of("", "C:\\source.ps"), ShellArguments.tokenize("\"\" C:\\source.ps"));
    }
}
