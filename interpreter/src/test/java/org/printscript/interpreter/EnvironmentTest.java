package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnvironmentTest {

    @Test
    void aDeclaredVariableCanBeRead() {
        Environment environment = new Environment().declare("x", "number", 12.0).orElseThrow();

        Variable variable = environment.read("x").orElseThrow();

        assertEquals("number", variable.declaredType());
        assertEquals(12.0, variable.value());
    }

    @Test
    void redeclaringAnExistingNameIsRejected() {
        Environment environment = new Environment().declare("x", "number", 12.0).orElseThrow();

        assertTrue(environment.declare("x", "string", "Joe").isEmpty());
    }

    @Test
    void assigningToAnUndeclaredNameIsRejected() {
        assertTrue(new Environment().assign("x", 12.0).isEmpty());
    }

    @Test
    void readingAnUndeclaredNameIsRejected() {
        assertTrue(new Environment().read("x").isEmpty());
    }
}
