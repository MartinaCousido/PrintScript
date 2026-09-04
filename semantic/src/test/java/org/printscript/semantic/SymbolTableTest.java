package org.printscript.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class SymbolTableTest {

    @Test
    void anEmptyTableKnowsNoName() {
        assertEquals(Optional.empty(), new SymbolTable().read("x"));
    }

    @Test
    void aDeclaredNameIsReadBackWithItsType() {
        SymbolTable table = new SymbolTable().declare("x", Type.NUMBER).orElseThrow();

        assertEquals(Optional.of(Type.NUMBER), table.read("x"));
    }

    @Test
    void declaringTheSameNameTwiceIsRejected() {
        SymbolTable table = new SymbolTable().declare("x", Type.NUMBER).orElseThrow();

        assertEquals(Optional.empty(), table.declare("x", Type.STRING));
    }

    @Test
    void declaringDoesNotModifyTheTableItCameFrom() {
        SymbolTable original = new SymbolTable();

        SymbolTable derived = original.declare("x", Type.STRING).orElseThrow();

        assertEquals(Optional.empty(), original.read("x"));
        assertEquals(Optional.of(Type.STRING), derived.read("x"));
    }

    @Test
    void differentNamesCoexist() {
        SymbolTable table =
                new SymbolTable()
                        .declare("x", Type.NUMBER)
                        .orElseThrow()
                        .declare("y", Type.STRING)
                        .orElseThrow();

        assertEquals(Optional.of(Type.NUMBER), table.read("x"));
        assertEquals(Optional.of(Type.STRING), table.read("y"));
        assertTrue(table.read("z").isEmpty());
    }
}
