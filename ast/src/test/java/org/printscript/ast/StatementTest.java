package org.printscript.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;

class StatementTest {

    private static final String NUMBER = "number";

    private static Range rangeAt(int startColumn, int endColumn) {
        return new Range(new Position(1, startColumn), new Position(1, endColumn));
    }

    private static String describe(Statement statement) {
        return switch (statement) {
            case VariableDeclaration declaration -> "declaration:" + declaration.name().name();
            case ConstDeclaration declaration -> "const:" + declaration.name().name();
            case AssignmentStatement assignment -> "assignment:" + assignment.name().name();
            case PrintlnStatement _ -> "println";
        };
    }

    @Test
    void declarationWithInitializerCarriesTheExpression() {
        Expression initializer = new NumberLiteral(8.0, rangeAt(14, 14));
        VariableDeclaration declaration =
                new VariableDeclaration(
                        new Identifier("x", rangeAt(5, 5)),
                        new TypeName(NUMBER, rangeAt(8, 13)),
                        Optional.of(initializer),
                        rangeAt(1, 14));

        assertTrue(declaration.initializer().isPresent());
        assertEquals(initializer, declaration.initializer().get());
        assertEquals("x", declaration.name().name());
        assertEquals(NUMBER, declaration.type().name());
        assertEquals(rangeAt(8, 13), declaration.type().range());
        assertEquals("declaration:x", describe(declaration));
    }

    @Test
    void aConstantIsItsOwnKindOfStatement() {
        // es un nodo propio y no un flag: por eso el switch lo distingue sin mirar nada
        ConstDeclaration constant =
                new ConstDeclaration(
                        new Identifier("x", rangeAt(7, 7)),
                        new TypeName(NUMBER, rangeAt(10, 15)),
                        Optional.of(new NumberLiteral(8.0, rangeAt(19, 19))),
                        rangeAt(1, 20));

        assertEquals("const:x", describe(constant));
        assertEquals(rangeAt(1, 20), constant.range());
    }

    @Test
    void declarationWithoutInitializerHasAnEmptyOptional() {
        VariableDeclaration declaration =
                new VariableDeclaration(
                        new Identifier("y", rangeAt(5, 5)),
                        new TypeName("string", rangeAt(8, 13)),
                        Optional.empty(),
                        rangeAt(1, 13));

        assertFalse(declaration.initializer().isPresent());
        assertEquals(Optional.empty(), declaration.initializer());
    }

    @Test
    void assignmentAndPrintlnExposeTheirRangeAndAreExhaustive() {
        Expression value = new Identifier("x", rangeAt(5, 5));
        AssignmentStatement assignment =
                new AssignmentStatement(new Identifier("y", rangeAt(1, 1)), value, rangeAt(1, 5));
        PrintlnStatement println = new PrintlnStatement(value, rangeAt(1, 12));

        assertEquals("y", assignment.name().name());
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

    @Test
    void declarationKeepsTheRangeOfItsOwnName() {
        Identifier name = new Identifier("x", rangeAt(5, 5));
        VariableDeclaration declaration =
                new VariableDeclaration(
                        name,
                        new TypeName(NUMBER, rangeAt(8, 13)),
                        Optional.empty(),
                        rangeAt(1, 13));

        assertEquals(name, declaration.name());
        assertEquals(rangeAt(5, 5), declaration.name().range());
    }

    @Test
    void assignmentKeepsTheRangeOfItsOwnName() {
        Identifier name = new Identifier("y", rangeAt(1, 1));
        AssignmentStatement assignment =
                new AssignmentStatement(name, new NumberLiteral(5.0, rangeAt(5, 5)), rangeAt(1, 5));

        assertEquals(name, assignment.name());
        assertEquals(rangeAt(1, 1), assignment.name().range());
    }
}
