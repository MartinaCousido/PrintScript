package org.printscript.linter.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.printscript.linter.Violation;

class PrintlnArgumentRuleTest {

    private static final Range ANY = new Range(new Position(1, 1), new Position(1, 1));
    private static final String NOMBRE = "nombre";

    private final PrintlnArgumentRule rule = new PrintlnArgumentRule();

    private static Expression sum(Range range) {
        return new BinaryExpression(
                new Identifier(NOMBRE, ANY), Operator.PLUS, new NumberLiteral(1.0, ANY), range);
    }

    @Test
    void printlnWithAnIdentifierIsAllowed() {
        Statement println = new PrintlnStatement(new Identifier(NOMBRE, ANY), ANY);

        assertTrue(rule.check(println).isEmpty());
    }

    @Test
    void printlnWithAStringLiteralIsAllowed() {
        Statement println = new PrintlnStatement(new StringLiteral("hola", ANY), ANY);

        assertTrue(rule.check(println).isEmpty());
    }

    @Test
    void printlnWithANumberLiteralIsAllowed() {
        Statement println = new PrintlnStatement(new NumberLiteral(168.0, ANY), ANY);

        assertTrue(rule.check(println).isEmpty());
    }

    @Test
    void printlnWithAnExpressionIsAViolation() {
        Statement println = new PrintlnStatement(sum(ANY), ANY);

        assertEquals(1, rule.check(println).size());
    }

    @Test
    void theViolationPointsAtTheArgumentAndNotAtTheWholeCall() {
        Range argumentRange = new Range(new Position(2, 9), new Position(2, 20));
        Range callRange = new Range(new Position(2, 1), new Position(2, 22));
        Statement println = new PrintlnStatement(sum(argumentRange), callRange);

        assertEquals(argumentRange, rule.check(println).get(0).range());
    }

    @Test
    void theMessageNamesPrintln() {
        Violation violation = rule.check(new PrintlnStatement(sum(ANY), ANY)).get(0);

        assertTrue(violation.message().contains("println"), violation.message());
    }

    @Test
    void anExpressionOutsideAPrintlnIsNotThisRulesBusiness() {
        Statement assignment = new AssignmentStatement(new Identifier(NOMBRE, ANY), sum(ANY), ANY);
        Statement declaration =
                new VariableDeclaration(
                        new Identifier(NOMBRE, ANY),
                        new TypeName("number", ANY),
                        Optional.of(sum(ANY)),
                        ANY);

        assertTrue(rule.check(assignment).isEmpty());
        assertTrue(rule.check(declaration).isEmpty());
    }
}
