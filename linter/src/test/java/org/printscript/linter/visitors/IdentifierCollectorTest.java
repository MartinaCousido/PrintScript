package org.printscript.linter.visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Position;
import org.printscript.common.Range;

class IdentifierCollectorTest {

    private static final Range ANY = new Range(new Position(1, 1), new Position(1, 1));
    private static final String NUMBER = "number";
    private static final String TOTAL = "total";
    private static final String PRECIO = "precio";

    private final IdentifierCollector collector = new IdentifierCollector();

    private static Identifier identifier(String name) {
        return new Identifier(name, ANY);
    }

    private List<String> namesIn(Statement statement) {
        return statement.accept(collector).stream().map(Identifier::name).toList();
    }

    @Test
    void collectsTheNameOfADeclaration() {
        Statement declaration =
                new VariableDeclaration(
                        identifier(TOTAL), new TypeName(NUMBER, ANY), Optional.empty(), ANY);

        assertEquals(List.of(TOTAL), namesIn(declaration));
    }

    @Test
    void collectsTheNameAndTheIdentifiersOfTheInitializer() {
        Statement declaration =
                new VariableDeclaration(
                        identifier(TOTAL),
                        new TypeName(NUMBER, ANY),
                        Optional.of(identifier(PRECIO)),
                        ANY);

        assertEquals(List.of(TOTAL, PRECIO), namesIn(declaration));
    }

    @Test
    void collectsTheNameAndTheIdentifiersOfAnAssignment() {
        Statement assignment = new AssignmentStatement(identifier(TOTAL), identifier(PRECIO), ANY);

        assertEquals(List.of(TOTAL, PRECIO), namesIn(assignment));
    }

    @Test
    void collectsTheIdentifiersInsideAPrintln() {
        Statement println = new PrintlnStatement(identifier("nombre"), ANY);

        assertEquals(List.of("nombre"), namesIn(println));
    }

    @Test
    void descendsIntoBothSidesOfANestedBinaryExpression() {
        Expression nested =
                new BinaryExpression(
                        identifier("a"),
                        Operator.PLUS,
                        new BinaryExpression(
                                identifier("b"), Operator.MULTIPLY, identifier("c"), ANY),
                        ANY);
        Statement println = new PrintlnStatement(nested, ANY);

        assertEquals(List.of("a", "b", "c"), namesIn(println));
    }

    @Test
    void literalsContributeNoIdentifiers() {
        Statement println =
                new PrintlnStatement(
                        new BinaryExpression(
                                new StringLiteral("hola", ANY),
                                Operator.PLUS,
                                new NumberLiteral(1.0, ANY),
                                ANY),
                        ANY);

        assertTrue(namesIn(println).isEmpty());
    }

    @Test
    void everyIdentifierKeepsItsOwnRange() {
        Range nameRange = new Range(new Position(2, 5), new Position(2, 9));
        Range valueRange = new Range(new Position(2, 13), new Position(2, 17));
        Statement assignment =
                new AssignmentStatement(
                        new Identifier(TOTAL, nameRange), new Identifier(PRECIO, valueRange), ANY);

        List<Identifier> collected = assignment.accept(collector);

        assertEquals(nameRange, collected.get(0).range());
        assertEquals(valueRange, collected.get(1).range());
    }
}
