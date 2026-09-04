package org.printscript.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.printscript.ast.Identifier;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.common.Version;

class SemanticAnalyzerTest {

    private static final String MISSING_SEMICOLON = "Expected ';'";

    // fuente de statements en memoria, para probar la etapa sin lexer ni parser
    private static final class Statements implements PrintScriptIterator<Result<Statement>> {

        private final List<Result<Statement>> results;
        private int index;

        private Statements(List<Result<Statement>> results) {
            this.results = results;
        }

        @Override
        public boolean hasNext() {
            return index < results.size();
        }

        @Override
        public Result<Statement> getNext() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Result<Statement> next = results.get(index);
            index = index + 1;
            return next;
        }
    }

    // pasa por la factory a propósito: así el cableado por versión también queda probado
    private static SemanticAnalyzer analyzerOver(PrintScriptIterator<Result<Statement>> source) {
        return SemanticFactory.create(source, Version.V10);
    }

    private static Range rangeAt(int line, int column) {
        Position position = new Position(line, column);
        return new Range(position, position);
    }

    // un println de un literal: sintáctica y semánticamente válido siempre
    private static Statement println(String literal, Range range) {
        return new PrintlnStatement(new StringLiteral(literal, range), range);
    }

    @Test
    void aSyntaxErrorTravelsThroughUnchanged() {
        Failure<Statement> syntaxError = new Failure<>(MISSING_SEMICOLON, rangeAt(3, 7));
        SemanticAnalyzer analyzer = analyzerOver(new Statements(List.of(syntaxError)));

        Result<Statement> result = analyzer.getNext();

        assertSame(syntaxError, result);
    }

    @Test
    void aSyntaxErrorKeepsItsMessageAndItsRange() {
        SemanticAnalyzer analyzer =
                analyzerOver(
                        new Statements(List.of(new Failure<>(MISSING_SEMICOLON, rangeAt(3, 7)))));

        Failure<Statement> failure = assertInstanceOf(Failure.class, analyzer.getNext());

        assertEquals(MISSING_SEMICOLON, failure.message());
        assertEquals(rangeAt(3, 7), failure.range());
    }

    @Test
    void aStatementThatPassesTheRulesTravelsThroughUnchanged() {
        Statement first = println("a", rangeAt(1, 1));
        Statement second = println("b", rangeAt(2, 1));
        SemanticAnalyzer analyzer =
                analyzerOver(new Statements(List.of(new Success<>(first), new Success<>(second))));

        assertEquals(first, assertInstanceOf(Success.class, analyzer.getNext()).value());
        assertEquals(second, assertInstanceOf(Success.class, analyzer.getNext()).value());
        assertFalse(analyzer.hasNext());
    }

    @Test
    void anErrorDoesNotStopTheStatementsThatFollow() {
        Statement valid = println("a", rangeAt(2, 1));
        SemanticAnalyzer analyzer =
                analyzerOver(
                        new Statements(
                                List.of(
                                        new Failure<>(MISSING_SEMICOLON, rangeAt(1, 5)),
                                        new Success<>(valid))));

        assertInstanceOf(Failure.class, analyzer.getNext());
        assertInstanceOf(Success.class, analyzer.getNext());
    }

    @Test
    void aSemanticErrorComesOutAsAFailureWithItsMessageAndRange() {
        Range use = rangeAt(2, 9);
        Statement printUndeclared = new PrintlnStatement(new Identifier("x", use), rangeAt(2, 1));
        SemanticAnalyzer analyzer =
                analyzerOver(new Statements(List.of(new Success<>(printUndeclared))));

        Failure<Statement> failure = assertInstanceOf(Failure.class, analyzer.getNext());

        assertEquals("Variable no declarada: x", failure.message());
        assertEquals(use, failure.range());
    }

    @Test
    void aSemanticErrorDoesNotCondemnTheStatementsThatFollow() {
        Statement printUndeclared =
                new PrintlnStatement(new Identifier("x", rangeAt(1, 9)), rangeAt(1, 1));
        Statement valid = println("hola", rangeAt(2, 1));
        SemanticAnalyzer analyzer =
                analyzerOver(
                        new Statements(
                                List.of(new Success<>(printUndeclared), new Success<>(valid))));

        assertInstanceOf(Failure.class, analyzer.getNext());
        assertInstanceOf(Success.class, analyzer.getNext());
    }

    @Test
    void followsTheSourceWhenAskedWhetherThereIsMore() {
        SemanticAnalyzer empty = analyzerOver(new Statements(List.of()));
        SemanticAnalyzer withOne =
                analyzerOver(new Statements(List.of(new Success<>(println("a", rangeAt(1, 1))))));

        assertFalse(empty.hasNext());
        assertTrue(withOne.hasNext());
    }
}
