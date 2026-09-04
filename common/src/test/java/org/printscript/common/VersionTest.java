package org.printscript.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class VersionTest {

    @Test
    void theSystemKnowsExactlyTheseVersions() {
        // no es una prueba de cobertura: agregar una versión rompe acá, y eso es el
        // recordatorio de que hay que cablearla en la factory de cada módulo que sabe
        // del lenguaje (lexer, parser, semantic, interpreter)
        assertEquals(List.of(Version.V10, Version.V11), List.of(Version.values()));
    }

    @Test
    void aVersionIsReadBackFromItsName() {
        assertEquals(Version.V10, Version.valueOf("V10"));
        assertEquals(Version.V11, Version.valueOf("V11"));
    }
}
