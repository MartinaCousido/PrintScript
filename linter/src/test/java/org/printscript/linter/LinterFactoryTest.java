package org.printscript.linter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.ReadInput;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.linter.config.IdentifierFormat;
import org.printscript.linter.config.LinterConfiguration;

class LinterFactoryTest {

    private static final Range ANY = new Range(new Position(1, 1), new Position(1, 1));
    private static final String MAL = "mal_escrito";

    private static final class OneStatement implements Iterator<Result<Statement>> {

        private final Statement statement;
        private boolean served;

        private OneStatement(Statement statement) {
            this.statement = statement;
        }

        @Override
        public boolean hasNext() {
            return !served;
        }

        @Override
        public Result<Statement> next() {
            served = true;
            return new Success<>(statement);
        }
    }

    private static Statement printlnOfASum(String name) {
        return new PrintlnStatement(
                new BinaryExpression(
                        new Identifier(name, ANY), Operator.PLUS, new NumberLiteral(1.0, ANY), ANY),
                ANY);
    }

    private static List<Violation> violationsOf(Statement statement, LinterConfiguration config) {
        Linter linter = LinterFactory.create(new OneStatement(statement), config);
        if (linter.next() instanceof Success<List<Violation>> success) {
            return success.value();
        }
        throw new AssertionError("se esperaba un Success");
    }

    @Test
    void withEverythingOffNothingIsReported() {
        LinterConfiguration config = new LinterConfiguration(Optional.empty(), false, false);

        assertTrue(violationsOf(printlnOfASum(MAL), config).isEmpty());
    }

    @Test
    void withOnlyTheFormatRuleOnOnlyTheIdentifierIsReported() {
        LinterConfiguration config =
                new LinterConfiguration(Optional.of(IdentifierFormat.CAMEL_CASE), false, false);

        List<Violation> violations = violationsOf(printlnOfASum(MAL), config);

        assertEquals(1, violations.size());
        assertTrue(violations.get(0).message().contains(MAL), violations.get(0).message());
    }

    @Test
    void withOnlyThePrintlnRuleOnOnlyTheArgumentIsReported() {
        LinterConfiguration config = new LinterConfiguration(Optional.empty(), true, false);

        List<Violation> violations = violationsOf(printlnOfASum(MAL), config);

        assertEquals(1, violations.size());
        assertTrue(violations.get(0).message().contains("println"), violations.get(0).message());
    }

    @Test
    void withBothRulesOnBothAreReported() {
        LinterConfiguration config =
                new LinterConfiguration(Optional.of(IdentifierFormat.CAMEL_CASE), true, false);

        assertEquals(2, violationsOf(printlnOfASum(MAL), config).size());
    }

    @Test
    void aCleanStatementIsReportedCleanWithEveryRuleOn() {
        LinterConfiguration config =
                new LinterConfiguration(Optional.of(IdentifierFormat.CAMEL_CASE), true, false);
        Statement println = new PrintlnStatement(new Identifier("bienEscrito", ANY), ANY);

        assertTrue(violationsOf(println, config).isEmpty());
    }

    // el identificador "mal_escrito" viola camel case pero cumple snake case: sirve para
    // comprobar que la factory elige el checker que corresponde y no siempre el mismo
    @Test
    void snakeCaseProducesItsOwnChecker() {
        LinterConfiguration snake =
                new LinterConfiguration(Optional.of(IdentifierFormat.SNAKE_CASE), false, false);
        LinterConfiguration camel =
                new LinterConfiguration(Optional.of(IdentifierFormat.CAMEL_CASE), false, false);

        assertEquals(List.of(), violationsOf(printlnOfASum(MAL), snake));
        assertEquals(1, violationsOf(printlnOfASum(MAL), camel).size());
    }

    @Test
    void theReadInputRuleIsWiredOnlyWhenItIsOn() {
        Statement statement =
                new PrintlnStatement(
                        new ReadInput(
                                new BinaryExpression(
                                        new StringLiteral("a", ANY),
                                        Operator.PLUS,
                                        new StringLiteral("b", ANY),
                                        ANY),
                                ANY),
                        ANY);

        LinterConfiguration encendida = new LinterConfiguration(Optional.empty(), false, true);
        LinterConfiguration apagada = new LinterConfiguration(Optional.empty(), false, false);

        assertEquals(1, violationsOf(statement, encendida).size());
        assertEquals(List.of(), violationsOf(statement, apagada));
    }
}
