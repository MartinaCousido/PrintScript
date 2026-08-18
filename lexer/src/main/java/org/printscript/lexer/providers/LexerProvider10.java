package org.printscript.lexer.providers;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.printscript.lexer.Lexer;
import org.printscript.lexer.constructors.KeywordTokenConstructor;
import org.printscript.lexer.constructors.NumberTokenConstructor;
import org.printscript.lexer.constructors.StringTokenConstructor;
import org.printscript.lexer.constructors.SymbolTokenConstructor;
import org.printscript.token.TokenType;
//tod lo que sabe la versión 1.0 vive acá: las 4 palabras reservadas y la tabla de simbolos
public final class LexerProvider10 implements LexerProvider {

    @Override
    public Lexer provide(Iterator<String> lines) {
        return new Lexer(
            lines,
            List.of(
                new StringTokenConstructor(),
                new NumberTokenConstructor(),
                new KeywordTokenConstructor(keywords()),
                new SymbolTokenConstructor(symbols())));
    }

    private static Map<String, TokenType> keywords() {
        Map<String, TokenType> keywords = new LinkedHashMap<>();
        keywords.put("let", TokenType.KEYWORD);
        keywords.put("println", TokenType.KEYWORD);
        keywords.put("number", TokenType.KEYWORD);
        keywords.put("string", TokenType.KEYWORD);
        return keywords;
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
