package org.printscript.linter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.Identifier;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.common.ErrorCode;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.linter.rules.LintRule;

class LinterTest {

    private static final Range ANY = new Range(new Position(1, 1), new Position(1, 1));

    private static final class ListStatements implements Iterator<Result<Statement>> {

        private final List<Result<Statement>> items;
        private int index;

        private ListStatements(List<Result<Statement>> items) {
            this.items = items;
        }

        @Override
        public boolean hasNext() {
            return index < items.size();
        }

        @Override
        public Result<Statement> next() {
            Result<Statement> item = items.get(index);
            index += 1;
            return item;
        }

        private int consumed() {
            return index;
        }
    }

    private static final class AlwaysComplains implements LintRule {

        private final String message;

        private AlwaysComplains(String message) {
            this.message = message;
        }

        @Override
        public List<Violation> check(Statement statement) {
            return List.of(new Violation("siempre", message, statement.range()));
        }
    }

    private static final class NeverComplains implements LintRule {

        @Override
        public List<Violation> check(Statement statement) {
            return List.of();
        }
    }

    private static Statement println(String name) {
        return new PrintlnStatement(new Identifier(name, ANY), ANY);
    }

    private static List<Violation> violationsOf(Result<List<Violation>> result) {
        if (result instanceof Success<List<Violation>> success) {
            return success.value();
        }
        throw new AssertionError("se esperaba un Success y vino " + result);
    }

    private static Failure<List<Violation>> failureOf(Result<List<Violation>> result) {
        if (result instanceof Failure<List<Violation>> failure) {
            return failure;
        }
        throw new AssertionError("se esperaba un Failure y vino " + result);
    }

    @Test
    void aStatementWithoutViolationsYieldsAnEmptyList() {
        Linter linter =
                new Linter(
                        new ListStatements(List.of(new Success<>(println("x")))),
                        List.of(new NeverComplains()));

        assertTrue(violationsOf(linter.next()).isEmpty());
    }

    @Test
    void everyRuleIsAppliedAndTheFirstViolationDoesNotStopTheRest() {
        Linter linter =
                new Linter(
                        new ListStatements(List.of(new Success<>(println("x")))),
                        List.of(new AlwaysComplains("primera"), new AlwaysComplains("segunda")));

        List<String> messages =
                violationsOf(linter.next()).stream().map(Violation::message).toList();

        assertEquals(List.of("primera", "segunda"), messages);
    }

    @Test
    void aViolationCarriesTheRangeOfWhatItIsAbout() {
        Range range = new Range(new Position(3, 5), new Position(3, 12));
        Statement statement = new PrintlnStatement(new Identifier("x", ANY), range);
        Linter linter =
                new Linter(
                        new ListStatements(List.of(new Success<>(statement))),
                        List.of(new AlwaysComplains("algo")));

        assertEquals(range, violationsOf(linter.next()).get(0).range());
    }

    @Test
    void aParserFailureIsForwardedWithItsMessageAndRange() {
        Range range = new Range(new Position(2, 1), new Position(2, 9));
        Linter linter =
                new Linter(
                        new ListStatements(
                                List.of(
                                        new Failure<>(
                                                ErrorCode.MISSING_SEMICOLON, "falta ';'", range))),
                        List.of(new AlwaysComplains("algo")));

        Failure<List<Violation>> failure = failureOf(linter.next());

        assertEquals("falta ';'", failure.message());
        assertEquals(range, failure.range());
    }

    @Test
    void aStatementAfterAFailureIsStillAnalyzed() {
        Linter linter =
                new Linter(
                        new ListStatements(
                                List.of(
                                        new Failure<>(
                                                ErrorCode.UNRECOGNIZED_STATEMENT, "roto", ANY),
                                        new Success<>(println("x")))),
                        List.of(new AlwaysComplains("algo")));

        List<Result<List<Violation>>> results = new ArrayList<>();
        while (linter.hasNext()) {
            results.add(linter.next());
        }

        assertInstanceOf(Failure.class, results.get(0), "el primero es el error del parser");
        assertEquals(1, violationsOf(results.get(1)).size());
    }

    @Test
    void eachCallConsumesExactlyOneStatementFromTheSource() {
        ListStatements source =
                new ListStatements(
                        List.of(new Success<>(println("x")), new Success<>(println("y"))));
        Linter linter = new Linter(source, List.of(new NeverComplains()));

        linter.next();

        assertEquals(1, source.consumed());
        assertTrue(linter.hasNext());
    }

    @Test
    void hasNextIsFalseOnceTheSourceIsExhausted() {
        Linter linter = new Linter(new ListStatements(List.of()), List.of(new NeverComplains()));

        assertFalse(linter.hasNext());
    }
}
