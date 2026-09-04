package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Statement;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.common.Version;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// qué declaradores y qué sentencias existen es conocimiento de la versión y vive acá:
// estos tests fijan que el ParserFactory sea el único que lo sabe
class ParserFactoryTest {

    private static final String NUMBER = "number";

    private static Token token(TokenType type, String value, int column) {
        return new Token(
                type,
                value,
                new Range(new Position(1, column), new Position(1, column + value.length() - 1)));
    }

    private static Result<Statement> parseFirst(Version version, Token... tokens) {
        Parser parser =
                ParserFactory.create(
                        new TokenBuffer(new ListTokenIterator(List.of(tokens))), version);
        return parser.getNext();
    }

    // const x: number = 1;
    private static Token[] constantDeclaration() {
        return new Token[] {
            token(TokenType.KEYWORD, "const", 1),
            token(TokenType.IDENTIFIER, "x", 7),
            token(TokenType.PUNCTUATION, ":", 8),
            token(TokenType.KEYWORD, NUMBER, 10),
            token(TokenType.OPERATOR, "=", 17),
            token(TokenType.NUMBER, "1", 19),
            token(TokenType.PUNCTUATION, ";", 20)
        };
    }

    @Test
    void version11BuildsAConstDeclaration() {
        Result<Statement> result = parseFirst(Version.V11, constantDeclaration());

        assertInstanceOf(ConstDeclaration.class, assertInstanceOf(Success.class, result).value());
    }

    @Test
    void version10DoesNotKnowTheConstDeclarator() {
        Failure<?> failure =
                assertInstanceOf(Failure.class, parseFirst(Version.V10, constantDeclaration()));

        assertEquals("Unrecognized statement", failure.message());
    }

    @Test
    void bothVersionsBuildTheLetDeclaration() {
        Token[] declaration = {
            token(TokenType.KEYWORD, "let", 1),
            token(TokenType.IDENTIFIER, "x", 5),
            token(TokenType.PUNCTUATION, ":", 6),
            token(TokenType.KEYWORD, NUMBER, 8),
            token(TokenType.PUNCTUATION, ";", 14)
        };

        for (Version version : Version.values()) {
            assertInstanceOf(
                    VariableDeclaration.class,
                    assertInstanceOf(Success.class, parseFirst(version, declaration)).value());
        }
    }

    @Test
    void aParenthesizedBooleanKeepsItsValueAndTakesTheRangeOfTheParentheses() {
        // let ok: boolean = (true);
        Result<Statement> result =
                parseFirst(
                        Version.V11,
                        token(TokenType.KEYWORD, "let", 1),
                        token(TokenType.IDENTIFIER, "ok", 5),
                        token(TokenType.PUNCTUATION, ":", 7),
                        token(TokenType.KEYWORD, "boolean", 9),
                        token(TokenType.OPERATOR, "=", 17),
                        token(TokenType.PUNCTUATION, "(", 19),
                        token(TokenType.BOOLEAN, "true", 20),
                        token(TokenType.PUNCTUATION, ")", 24),
                        token(TokenType.PUNCTUATION, ";", 25));

        VariableDeclaration declaration =
                (VariableDeclaration) assertInstanceOf(Success.class, result).value();
        BooleanLiteral literal = (BooleanLiteral) declaration.initializer().orElseThrow();
        assertTrue(literal.value());
        assertEquals(19, literal.range().start().column());
        assertEquals(24, literal.range().end().column());
    }
}
