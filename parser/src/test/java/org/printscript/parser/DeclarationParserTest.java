package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Statement;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class DeclarationParserTest {

    private static final String LET = "let";
    private static final String NUMBER = "number";

    private final DeclarationParser parser =
            new DeclarationParser(new ExpressionBuilder(), Map.of("let", VariableDeclaration::new));

    private static Range range(int startColumn, int endColumn) {
        return new Range(new Position(1, startColumn), new Position(1, endColumn));
    }

    private static Token token(TokenType type, String value, int column) {
        return new Token(type, value, range(column, column + value.length() - 1));
    }

    private static TokenBuffer bufferOf(Token... tokens) {
        return new TokenBuffer(new ListTokenIterator(List.of(tokens)));
    }

    @Test
    void matchesOnlyStatementsThatStartWithLet() {
        assertTrue(parser.matches(bufferOf(token(TokenType.KEYWORD, LET, 1))));
        assertFalse(parser.matches(bufferOf(token(TokenType.KEYWORD, "println", 1))));
        assertFalse(parser.matches(bufferOf(token(TokenType.IDENTIFIER, LET, 1))));
        assertFalse(parser.matches(bufferOf()));
    }

    @Test
    void parsesADeclarationWithInitializer() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.KEYWORD, LET, 1),
                        token(TokenType.IDENTIFIER, "x", 5),
                        token(TokenType.PUNCTUATION, ":", 6),
                        token(TokenType.KEYWORD, NUMBER, 8),
                        token(TokenType.OPERATOR, "=", 15),
                        token(TokenType.NUMBER, "5", 17),
                        token(TokenType.PUNCTUATION, ";", 18));

        Result<Statement> result = parser.parse(tokens);

        Statement expected =
                new VariableDeclaration(
                        new Identifier("x", range(5, 5)),
                        new TypeName(NUMBER, range(8, 13)),
                        Optional.of(new NumberLiteral(5.0, range(17, 17))),
                        range(1, 18));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void parsesADeclarationWithoutInitializer() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.KEYWORD, LET, 1),
                        token(TokenType.IDENTIFIER, "x", 5),
                        token(TokenType.PUNCTUATION, ":", 6),
                        token(TokenType.KEYWORD, NUMBER, 8),
                        token(TokenType.PUNCTUATION, ";", 15));

        Result<Statement> result = parser.parse(tokens);

        Statement expected =
                new VariableDeclaration(
                        new Identifier("x", range(5, 5)),
                        new TypeName(NUMBER, range(8, 13)),
                        Optional.empty(),
                        range(1, 15));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void keepsTheTypeAnnotationAsWritten() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.KEYWORD, LET, 1),
                        token(TokenType.IDENTIFIER, "x", 5),
                        token(TokenType.PUNCTUATION, ":", 6),
                        token(TokenType.IDENTIFIER, "Foo", 8),
                        token(TokenType.PUNCTUATION, ";", 12));

        Result<Statement> result = parser.parse(tokens);

        Success<Statement> success = assertInstanceOf(Success.class, result);
        VariableDeclaration declaration =
                assertInstanceOf(VariableDeclaration.class, success.value());
        assertEquals(new TypeName("Foo", range(8, 10)), declaration.type());
    }

    @Test
    void missingIdentifierFailsAtTheOffendingToken() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.KEYWORD, LET, 1),
                        token(TokenType.PUNCTUATION, ":", 5),
                        token(TokenType.KEYWORD, NUMBER, 7),
                        token(TokenType.PUNCTUATION, ";", 14));

        Result<Statement> result = parser.parse(tokens);

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(5, 5), failure.range());
    }

    @Test
    void missingColonFailsAtTheOffendingToken() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.KEYWORD, LET, 1),
                        token(TokenType.IDENTIFIER, "x", 5),
                        token(TokenType.OPERATOR, "=", 7),
                        token(TokenType.NUMBER, "5", 9),
                        token(TokenType.PUNCTUATION, ";", 10));

        Result<Statement> result = parser.parse(tokens);

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(7, 7), failure.range());
    }

    @Test
    void missingSemicolonFailsAfterTheLastTokenOfTheStatement() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.KEYWORD, LET, 1),
                        token(TokenType.IDENTIFIER, "x", 5),
                        token(TokenType.PUNCTUATION, ":", 6),
                        token(TokenType.KEYWORD, NUMBER, 8),
                        token(TokenType.OPERATOR, "=", 15),
                        token(TokenType.NUMBER, "5", 17));

        Result<Statement> result = parser.parse(tokens);

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(17, 17), failure.range());
    }

    @Test
    void emptyInitializerAfterTheEqualsSignFails() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.KEYWORD, LET, 1),
                        token(TokenType.IDENTIFIER, "x", 5),
                        token(TokenType.PUNCTUATION, ":", 6),
                        token(TokenType.KEYWORD, NUMBER, 8),
                        token(TokenType.OPERATOR, "=", 15),
                        token(TokenType.PUNCTUATION, ";", 17));

        Result<Statement> result = parser.parse(tokens);

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(17, 17), failure.range());
    }
}
