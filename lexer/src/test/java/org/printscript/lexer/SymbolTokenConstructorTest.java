package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.lexer.Constructor.SymbolTokenConstructor;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class SymbolTokenConstructorTest {

    private static Map<String, TokenType> symbolsOfVersion10() {
        Map<String, TokenType> symbols = new LinkedHashMap<>();
        symbols.put("+", TokenType.OPERATOR);
        symbols.put("-", TokenType.OPERATOR);
        symbols.put("*", TokenType.OPERATOR);
        symbols.put("/", TokenType.OPERATOR);
        symbols.put("=", TokenType.OPERATOR);
        symbols.put(";", TokenType.PUNCTUATION);
        symbols.put(":", TokenType.PUNCTUATION);
        symbols.put("(", TokenType.PUNCTUATION);
        symbols.put(")", TokenType.PUNCTUATION);
        return symbols;
    }

    private static Token buildAt(Map<String, TokenType> symbols, String rest, Position start) {
        Optional<Token> token = new SymbolTokenConstructor(symbols).tryBuild(rest, start);
        return token.orElseThrow();
    }

    @Test
    void everySymbolOfVersion10IsRecognizedWithItsCategoryAndRange() {
        Map<String, TokenType> symbols = symbolsOfVersion10();
        Position start = new Position(3, 7);

        for (Map.Entry<String, TokenType> symbol : symbols.entrySet()) {
            Token token = buildAt(symbols, symbol.getKey(), start);

            assertEquals(symbol.getValue(), token.type(), symbol.getKey());
            assertEquals(symbol.getKey(), token.value());
            assertEquals(new Range(start, new Position(3, 7)), token.range(), symbol.getKey());
        }
    }

    @Test
    void aTwoCharacterLexemeBeatsTheOneCharacterOneWithTheSamePrefix() {
        Map<String, TokenType> symbols = new LinkedHashMap<>();
        symbols.put("=", TokenType.OPERATOR);
        symbols.put("==", TokenType.OPERATOR);

        Token token = buildAt(symbols, "== 3", new Position(1, 1));

        assertEquals("==", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 2)), token.range());
    }
}
