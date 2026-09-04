package org.printscript.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// arma tokens a partir de lexemas separados por espacios, para que los tests del parser
// se lean como el código que representan. Las posiciones son sintéticas: acá se prueba
// la estructura del árbol, no el cálculo de rangos del lexer.
final class Lexemes {

    private static final Set<String> KEYWORDS =
            Set.of(
                    "let",
                    "const",
                    "if",
                    "else",
                    "println",
                    "number",
                    "string",
                    "boolean",
                    "readInput",
                    "readEnv");
    private static final Set<String> BOOLEANS = Set.of("true", "false");
    private static final Set<String> PUNCTUATION = Set.of("{", "}", "(", ")", ";", ":");
    private static final Set<String> OPERATORS = Set.of("+", "-", "*", "/", "=");

    private Lexemes() {}

    static TokenBuffer bufferOf(String source) {
        return new TokenBuffer(new ListTokenIterator(tokensOf(source)));
    }

    static List<Token> tokensOf(String source) {
        List<Token> tokens = new ArrayList<>();
        int column = 1;
        for (String lexeme : source.trim().split("\\s+")) {
            tokens.add(new Token(typeOf(lexeme), lexeme, rangeAt(column, lexeme.length())));
            column = column + lexeme.length() + 1;
        }
        return tokens;
    }

    private static TokenType typeOf(String lexeme) {
        if (BOOLEANS.contains(lexeme)) {
            return TokenType.BOOLEAN;
        }
        if (KEYWORDS.contains(lexeme)) {
            return TokenType.KEYWORD;
        }
        if (PUNCTUATION.contains(lexeme)) {
            return TokenType.PUNCTUATION;
        }
        if (OPERATORS.contains(lexeme)) {
            return TokenType.OPERATOR;
        }
        if (Character.isDigit(lexeme.charAt(0))) {
            return TokenType.NUMBER;
        }
        if (lexeme.startsWith("'")) {
            return TokenType.STRING;
        }
        return TokenType.IDENTIFIER;
    }

    private static Range rangeAt(int column, int length) {
        return new Range(new Position(1, column), new Position(1, column + length - 1));
    }
}
