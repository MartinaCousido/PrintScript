package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.printscript.ast.Identifier;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class ParserTest {

    private static final String GREET = "greet";

    private static final class FakeStatementParser implements StatementParser {

        private final String keyword;
        private final List<List<Token>> receivedStatements;

        private FakeStatementParser(String keyword) {
            this.keyword = keyword;
            this.receivedStatements = new ArrayList<>();
        }

        @Override
        public boolean matches(TokenBuffer tokens) {
            Optional<Token> first = tokens.lookahead(0);
            return first.isPresent()
                    && first.get().type() == TokenType.KEYWORD
                    && keyword.equals(first.get().value());
        }

        @Override
        public Result<Statement> parse(TokenBuffer tokens) {
            List<Token> received = new ArrayList<>();
            while (tokens.lookahead(0).isPresent()) {
                received.add(tokens.lookahead(0).get());
                tokens.commit(1);
            }
            receivedStatements.add(received);
            Range range =
                    new Range(
                            received.getFirst().range().start(), received.getLast().range().end());
            return new Success<>(new PrintlnStatement(new Identifier(keyword, range), range));
        }
    }

    private static Range rangeAt(int column) {
        Position position = new Position(1, column);
        return new Range(position, position);
    }

    private static Token token(TokenType type, String value, int column) {
        return new Token(type, value, rangeAt(column));
    }

    private static Token keyword(String value, int column) {
        return token(TokenType.KEYWORD, value, column);
    }

    private static Token identifier(String value, int column) {
        return token(TokenType.IDENTIFIER, value, column);
    }

    private static Token semicolon(int column) {
        return token(TokenType.PUNCTUATION, ";", column);
    }

    private static Parser parserOf(List<StatementParser> statementParsers, Token... tokens) {
        return parserOf(statementParsers, new SemicolonDelimiter(), tokens);
    }

    private static Parser parserOf(
            List<StatementParser> statementParsers, StatementDelimiter delimiter, Token... tokens) {
        return new Parser(
                new TokenBuffer(new ListTokenIterator(List.of(tokens))),
                statementParsers,
                delimiter);
    }

    @Test
    void dispatchesToTheStatementParserThatRecognizesTheStatement() {
        FakeStatementParser greet = new FakeStatementParser(GREET);
        FakeStatementParser farewell = new FakeStatementParser("farewell");
        Parser parser =
                parserOf(
                        List.of(greet, farewell),
                        keyword("farewell", 1),
                        identifier("x", 2),
                        semicolon(3));

        Result<Statement> result = parser.next();

        assertInstanceOf(Success.class, result);
        assertEquals(1, farewell.receivedStatements.size());
        assertEquals(0, greet.receivedStatements.size());
        assertFalse(parser.hasNext());
    }

    @Test
    void eachStatementParserReceivesOnlyTheTokensOfItsStatement() {
        FakeStatementParser greet = new FakeStatementParser(GREET);
        Parser parser =
                parserOf(
                        List.of(greet),
                        keyword(GREET, 1),
                        identifier("a", 2),
                        semicolon(3),
                        keyword(GREET, 4),
                        identifier("b", 5),
                        semicolon(6));

        assertInstanceOf(Success.class, parser.next());
        assertInstanceOf(Success.class, parser.next());
        assertFalse(parser.hasNext());

        assertEquals(
                List.of(
                        List.of(keyword(GREET, 1), identifier("a", 2), semicolon(3)),
                        List.of(keyword(GREET, 4), identifier("b", 5), semicolon(6))),
                greet.receivedStatements);
    }

    @Test
    void unrecognizedStatementProducesFailureWithTheRangeOfTheFirstToken() {
        Parser parser =
                parserOf(
                        List.of(new FakeStatementParser(GREET)),
                        keyword("unknown", 1),
                        identifier("x", 2),
                        semicolon(3));

        Result<Statement> result = parser.next();

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(rangeAt(1), failure.range());
        assertFalse(parser.hasNext());
    }

    @Test
    void recoversAtTheStatementFollowingAnUnrecognizedOne() {
        FakeStatementParser greet = new FakeStatementParser(GREET);
        Parser parser =
                parserOf(
                        List.of(greet),
                        keyword("unknown", 1),
                        identifier("x", 2),
                        semicolon(3),
                        keyword(GREET, 4),
                        semicolon(5));

        assertInstanceOf(Failure.class, parser.next());
        assertTrue(parser.hasNext());
        assertInstanceOf(Success.class, parser.next());
        assertEquals(List.of(List.of(keyword(GREET, 4), semicolon(5))), greet.receivedStatements);
        assertFalse(parser.hasNext());
    }

    @Test
    void missingSemicolonAtTheEndProducesFailureWithTheRangeOfTheLastToken() {
        Parser parser =
                parserOf(
                        List.of(new FakeStatementParser(GREET)),
                        keyword(GREET, 1),
                        identifier("x", 2));

        Result<Statement> result = parser.next();

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(rangeAt(2), failure.range());
        assertFalse(parser.hasNext());
    }

    @Test
    void theDelimiterDecidesWhereTheStatementEnds() {
        FakeStatementParser greet = new FakeStatementParser(GREET);
        // corta en el identificador en lugar del ';': el criterio no lo pone el Parser
        StatementDelimiter atIdentifier =
                new StatementDelimiter() {

                    @Override
                    public boolean endsStatement(
                            List<Token> accumulated,
                            Token current,
                            Supplier<Optional<Token>> next) {
                        return current.type() == TokenType.IDENTIFIER;
                    }

                    @Override
                    public String unterminatedMessage(List<Token> accumulated) {
                        return "Expected an identifier before the end of the stream";
                    }
                };
        Parser parser =
                parserOf(
                        List.of(greet),
                        atIdentifier,
                        keyword(GREET, 1),
                        identifier("a", 2),
                        semicolon(3));

        assertInstanceOf(Success.class, parser.next());

        assertEquals(
                List.of(List.of(keyword(GREET, 1), identifier("a", 2))), greet.receivedStatements);
        assertTrue(parser.hasNext());
    }

    @Test
    void theDelimiterProvidesTheUnterminatedStatementMessage() {
        StatementDelimiter neverEnds =
                new StatementDelimiter() {

                    @Override
                    public boolean endsStatement(
                            List<Token> accumulated,
                            Token current,
                            Supplier<Optional<Token>> next) {
                        return false;
                    }

                    @Override
                    public String unterminatedMessage(List<Token> accumulated) {
                        return "Expected the moon before the end of the stream";
                    }
                };
        Parser parser =
                parserOf(
                        List.of(new FakeStatementParser(GREET)),
                        neverEnds,
                        keyword(GREET, 1),
                        semicolon(2));

        Failure<Statement> failure = assertInstanceOf(Failure.class, parser.next());

        assertEquals("Expected the moon before the end of the stream", failure.message());
        assertEquals(rangeAt(2), failure.range());
    }

    @Test
    void theSemicolonDelimiterDoesNotAskForTheFollowingToken() {
        // pedir el token siguiente obliga al lexer a leer una línea más del fuente,
        // así que el criterio de 1.0 no lo puede consultar
        Supplier<Optional<Token>> forbidden =
                () -> {
                    throw new AssertionError("the 1.0 delimiter must not read ahead");
                };

        assertFalse(
                new SemicolonDelimiter().endsStatement(List.of(), identifier("x", 1), forbidden));
        assertTrue(new SemicolonDelimiter().endsStatement(List.of(), semicolon(2), forbidden));
    }

    @Test
    void nextWithoutPendingStatementsThrows() {
        Parser parser = parserOf(List.of(new FakeStatementParser(GREET)));

        assertFalse(parser.hasNext());
        assertThrows(NoSuchElementException.class, parser::next);
    }
}
