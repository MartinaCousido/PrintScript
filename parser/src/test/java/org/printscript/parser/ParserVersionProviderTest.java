package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.parser.providers.ParserProvider;
import org.printscript.parser.providers.ParserVersionProvider;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class ParserVersionProviderTest {

    private static Token token(TokenType type, String value, int column) {
        Range range = new Range(new Position(1, column), new Position(1, column + value.length() - 1));
        return new Token(type, value, range);
    }

    @Test
    void version10ProvidesAParserThatParsesTheThreeStatementsOfTheVersion() {
        ParserProvider provider = new ParserVersionProvider().providerFor("1.0");
        TokenBuffer buffer = new TokenBuffer(new ListTokenIterator(List.of(
            token(TokenType.KEYWORD, "let", 1),
            token(TokenType.IDENTIFIER, "x", 5),
            token(TokenType.PUNCTUATION, ":", 6),
            token(TokenType.KEYWORD, "number", 8),
            token(TokenType.PUNCTUATION, ";", 14),
            token(TokenType.IDENTIFIER, "x", 16),
            token(TokenType.OPERATOR, "=", 18),
            token(TokenType.NUMBER, "5", 20),
            token(TokenType.PUNCTUATION, ";", 21),
            token(TokenType.KEYWORD, "println", 23),
            token(TokenType.PUNCTUATION, "(", 30),
            token(TokenType.IDENTIFIER, "x", 31),
            token(TokenType.PUNCTUATION, ")", 32),
            token(TokenType.PUNCTUATION, ";", 33))));
        Parser parser = provider.provide(buffer);

        Result<Statement> declaration = parser.getNext();
        Result<Statement> assignment = parser.getNext();
        Result<Statement> println = parser.getNext();

        assertInstanceOf(VariableDeclaration.class, ((Success<Statement>) declaration).value());
        assertInstanceOf(AssignmentStatement.class, ((Success<Statement>) assignment).value());
        assertInstanceOf(PrintlnStatement.class, ((Success<Statement>) println).value());
        assertFalse(parser.hasNext());
    }

    @Test
    void anUnknownVersionFailsWithAClearMessage() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> new ParserVersionProvider().providerFor("9.9"));

        assertEquals("Versión de PrintScript desconocida: 9.9. Versiones soportadas: 1.0", error.getMessage());
    }
}
