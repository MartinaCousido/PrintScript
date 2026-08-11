package org.printscript.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;

class StatementTest {

    private static Range rangeAt(int startColumn, int endColumn) {
        return new Range(new Position(1, startColumn), new Position(1, endColumn));
    }

    private static String describe(Statement statement) {
        return switch (statement) {
            case VariableDeclaration declaration -> "declaration:" + declaration.name();
            case AssignmentStatement assignment -> "assignment:" + assignment.name();
            case PrintlnStatement println -> "println";
        };
    }

    @Test
    void declarationWithInitializerCarriesTheExpression() {
        Expression initializer = new NumberLiteral(8.0, rangeAt(14, 14));
        VariableDeclaration declaration = new VariableDeclaration(
            "x",
            new TypeName("number", rangeAt(8, 13)),
            Optional.of(initializer),
            rangeAt(1, 14));

        assertTrue(declaration.initializer().isPresent());
        assertEquals(initializer, declaration.initializer().get());
        assertEquals("x", declaration.name());
        assertEquals("number", declaration.type().name());
        assertEquals(rangeAt(8, 13), declaration.type().range());
        assertEquals("declaration:x", describe(declaration));
    }

    @Test
    void declarationWithoutInitializerHasAnEmptyOptional() {
        VariableDeclaration declaration = new VariableDeclaration(
            "y",
            new TypeName("string", rangeAt(8, 13)),
            Optional.empty(),
            rangeAt(1, 13));

        assertFalse(declaration.initializer().isPresent());
        assertEquals(Optional.empty(), declaration.initializer());
    }

    @Test
    void assignmentAndPrintlnExposeTheirRangeAndAreExhaustive() {
        Expression value = new Identifier("x", rangeAt(5, 5));
        AssignmentStatement assignment = new AssignmentStatement("y", value, rangeAt(1, 5));
        PrintlnStatement println = new PrintlnStatement(value, rangeAt(1, 12));

        assertEquals("y", assignment.name());
        assertEquals(value, assignment.expression());
        assertEquals(rangeAt(1, 5), assignment.range());

        assertEquals(value, println.expression());
        assertEquals(rangeAt(1, 12), println.range());

        assertEquals("assignment:y", describe(assignment));
        assertEquals("println", describe(println));
    }

    @Test
    void typeNameKeepsTheAnnotationAsWritten() {
        TypeName type = new TypeName("boolean", rangeAt(3, 9));

        assertEquals("boolean", type.name());
        assertEquals(rangeAt(3, 9), type.range());
    }
}
