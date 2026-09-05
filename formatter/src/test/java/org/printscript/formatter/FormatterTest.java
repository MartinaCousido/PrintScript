package org.printscript.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
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
import org.printscript.formatter.config.FormatterConfiguration;

class FormatterTest {

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
    }

    private static Statement declaration(String name, double value) {
        return new VariableDeclaration(
                new Identifier(name, ANY),
                new TypeName("number", ANY),
                Optional.of(new NumberLiteral(value, ANY)),
                ANY);
    }

    private static Statement println(String text) {
        return new PrintlnStatement(new StringLiteral(text, ANY), ANY);
    }

    private static Formatter formatterOf(
            FormatterConfiguration configuration, List<Result<Statement>> statements) {
        return new Formatter(new ListStatements(statements), configuration);
    }

    private static String formatAll(
            FormatterConfiguration configuration, List<Result<Statement>> statements) {
        Formatter formatter = formatterOf(configuration, statements);
        StringBuilder output = new StringBuilder();
        while (formatter.hasNext()) {
            output.append(((Success<String>) formatter.next()).value());
        }
        return output.toString();
    }

    private static final FormatterConfiguration SPACED =
            new FormatterConfiguration(false, true, true, 0);

    @Test
    void everyStatementEndsInALineBreakAfterTheSemicolon() {
        String output =
                formatAll(
                        SPACED,
                        List.of(
                                new Success<>(declaration("a", 12)),
                                new Success<>(declaration("b", 4))));

        assertEquals("let a: number = 12;\nlet b: number = 4;\n", output);
    }

    @Test
    void withoutTheRuleAPrintlnGetsNoBlankLineBeforeIt() {
        String output =
                formatAll(
                        SPACED,
                        List.of(
                                new Success<>(declaration("a", 12)),
                                new Success<>(println("hola"))));

        assertEquals("let a: number = 12;\nprintln(\"hola\");\n", output);
    }

    @Test
    void aPrintlnGetsTheConfiguredBlankLinesBeforeIt() {
        String output =
                formatAll(
                        new FormatterConfiguration(false, true, true, 2),
                        List.of(
                                new Success<>(declaration("a", 12)),
                                new Success<>(println("hola"))));

        assertEquals("let a: number = 12;\n\n\nprintln(\"hola\");\n", output);
    }

    @Test
    void aPrintlnThatOpensTheFileGetsNoBlankLinesBeforeIt() {
        String output =
                formatAll(
                        new FormatterConfiguration(false, true, true, 2),
                        List.of(new Success<>(println("hola"))));

        assertEquals("println(\"hola\");\n", output);
    }

    @Test
    void aFailureIsPassedThroughInsteadOfFormatted() {
        Formatter formatter = formatterOf(SPACED, List.of(new Failure<>("roto", ANY)));

        Result<String> result = formatter.next();

        Failure<String> failure = assertInstanceOf(Failure.class, result);
        assertEquals("roto", failure.message());
        assertEquals(ANY, failure.range());
    }

    @Test
    void thereIsNothingToFormatWhenThereAreNoStatements() {
        assertFalse(formatterOf(SPACED, List.of()).hasNext());
    }

    @Test
    void statementsAreFormattedOneAtATimeAndNotAllUpFront() {
        Formatter formatter =
                formatterOf(
                        SPACED,
                        List.of(
                                new Success<>(declaration("a", 12)),
                                new Success<>(declaration("b", 4))));

        assertEquals("let a: number = 12;\n", ((Success<String>) formatter.next()).value());
        assertTrue(formatter.hasNext());
    }
}
