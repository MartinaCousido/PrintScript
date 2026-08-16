package org.printscript.lexer.Provider;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.printscript.lexer.Constructor.KeywordTokenConstructor;
import org.printscript.lexer.Constructor.NumberTokenConstructor;
import org.printscript.lexer.Constructor.StringTokenConstructor;
import org.printscript.lexer.Constructor.SymbolTokenConstructor;
import org.printscript.lexer.Lexer;
import org.printscript.token.TokenType;
//tod lo que sabe la versión 1.0 vive acá: las 4 palabras reservadas y la tabla de simbolos
public final class LexerProvider10 implements LexerProvider {

    private static final Set<String> KEYWORDS = Set.of("let", "println", "number", "string");

    @Override
    public Lexer provide(Iterator<String> lines) {
        return new Lexer(
            lines,
            List.of(
                new StringTokenConstructor(),
                new NumberTokenConstructor(),
                new KeywordTokenConstructor(KEYWORDS),
                new SymbolTokenConstructor(symbols())));
    }

    private static Map<String, TokenType> symbols() {
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
}
