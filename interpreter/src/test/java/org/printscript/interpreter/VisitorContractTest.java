package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.runtime.OutputPrinter;

// El intérprete usa dos visitors sobre el mismo árbol: uno ejecuta sentencias y el otro
// evalúa expresiones. Cada uno tiene que rechazar explícitamente lo que no le toca, en
// vez de devolver algo raro. Estos tests fijan ese contrato.
class VisitorContractTest {

    private static final String NOT_A_STATEMENT = "No es una sentencia ejecutable";
    private static final String NOT_AN_EXPRESSION = "No es una expresión evaluable";
    private static final Range RANGE = new Range(new Position(1, 1), new Position(1, 4));

    private final Executor executor = new Executor(new Environment(), Ports.of(new Discarding()));
    private final ExpressionEvaluator evaluator =
            new ExpressionEvaluator(new Environment(), Ports.of(new Discarding()));

    private String executing(Expression expression) {
        return assertInstanceOf(Failure.class, expression.accept(executor)).message();
    }

    private String evaluating(Statement statement) {
        return assertInstanceOf(Failure.class, statement.accept(evaluator)).message();
    }

    @Test
    void theExecutorRejectsEveryExpression() {
        assertEquals(NOT_A_STATEMENT, executing(new NumberLiteral(1, RANGE)));
        assertEquals(NOT_A_STATEMENT, executing(new StringLiteral("x", RANGE)));
        assertEquals(NOT_A_STATEMENT, executing(new BooleanLiteral(true, RANGE)));
        assertEquals(NOT_A_STATEMENT, executing(new Identifier("x", RANGE)));
        assertEquals(
                NOT_A_STATEMENT,
                executing(
                        new BinaryExpression(
                                new NumberLiteral(1, RANGE),
                                Operator.PLUS,
                                new NumberLiteral(2, RANGE),
                                RANGE)));
    }

    @Test
    void theExecutorKeepsTheRangeOfWhatItRejected() {
        Failure<?> failure =
                assertInstanceOf(Failure.class, new BooleanLiteral(true, RANGE).accept(executor));

        assertEquals(RANGE, failure.range());
    }

    @Test
    void theEvaluatorRejectsEveryStatement() {
        assertEquals(
                NOT_AN_EXPRESSION,
                evaluating(
                        new VariableDeclaration(
                                new Identifier("x", RANGE),
                                new TypeName("number", RANGE),
                                Optional.empty(),
                                RANGE)));
        assertEquals(
                NOT_AN_EXPRESSION,
                evaluating(
                        new AssignmentStatement(
                                new Identifier("x", RANGE), new NumberLiteral(1, RANGE), RANGE)));
        assertEquals(
                NOT_AN_EXPRESSION,
                evaluating(new PrintlnStatement(new NumberLiteral(1, RANGE), RANGE)));
    }

    @Test
    void theEvaluatorGivesBackTheValueOfABooleanLiteral() {
        Result<Object> yes = new BooleanLiteral(true, RANGE).accept(evaluator);
        Result<Object> no = new BooleanLiteral(false, RANGE).accept(evaluator);

        assertEquals(true, assertInstanceOf(Success.class, yes).value());
        assertEquals(false, assertInstanceOf(Success.class, no).value());
    }

    @Test
    void anOperationOverABooleanNamesTheTypeInTheMessage() {
        // semantic lo rechaza antes, pero el intérprete no puede quedarse mudo si le
        // llega igual: la red de seguridad tiene que nombrar el tipo correcto
        Expression expression =
                new BinaryExpression(
                        new BooleanLiteral(true, RANGE),
                        Operator.MINUS,
                        new NumberLiteral(1, RANGE),
                        RANGE);

        assertEquals(
                "No se puede aplicar MINUS entre boolean y number",
                assertInstanceOf(Failure.class, expression.accept(evaluator)).message());
    }

    // no le interesa la salida, pero OutputPrinter ya no es funcional: son dos métodos
    private static final class Discarding implements OutputPrinter {

        @Override
        public void println(String line) {
            // descartado a propósito
        }

        @Override
        public void print(String text) {
            // descartado a propósito
        }
    }
}
