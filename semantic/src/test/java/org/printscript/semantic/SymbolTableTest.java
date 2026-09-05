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
        SymbolTable table =
                new SymbolTable().declare("x", new Symbol(Type.NUMBER, false)).orElseThrow();

        assertEquals(Optional.of(new Symbol(Type.NUMBER, false)), table.read("x"));
    }

    @Test
    void declaringTheSameNameTwiceIsRejected() {
        SymbolTable table =
                new SymbolTable().declare("x", new Symbol(Type.NUMBER, false)).orElseThrow();

        assertEquals(Optional.empty(), table.declare("x", new Symbol(Type.STRING, false)));
    }

    @Test
    void declaringDoesNotModifyTheTableItCameFrom() {
        SymbolTable original = new SymbolTable();

        SymbolTable derived = original.declare("x", new Symbol(Type.STRING, false)).orElseThrow();

        assertEquals(Optional.empty(), original.read("x"));
        assertEquals(Optional.of(new Symbol(Type.STRING, false)), derived.read("x"));
    }

    @Test
    void differentNamesCoexist() {
        SymbolTable table =
                new SymbolTable()
                        .declare("x", new Symbol(Type.NUMBER, false))
                        .orElseThrow()
                        .declare("y", new Symbol(Type.STRING, false))
                        .orElseThrow();

        assertEquals(Optional.of(new Symbol(Type.NUMBER, false)), table.read("x"));
        assertEquals(Optional.of(new Symbol(Type.STRING, false)), table.read("y"));
        assertTrue(table.read("z").isEmpty());
    }
}
