package org.printscript.lexer;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.printscript.common.Version;
import org.printscript.lexer.constructors.KeywordTokenConstructor;
import org.printscript.lexer.constructors.NumberTokenConstructor;
import org.printscript.lexer.constructors.StringTokenConstructor;
import org.printscript.lexer.constructors.SymbolTokenConstructor;
import org.printscript.token.TokenType;

// acá vive lo que el lexer sabe de cada versión
// el motor recibe constructores y tablas: no sabe que las versiones existen
public final class LexerFactory {

    private LexerFactory() {}

    public static Lexer create(Iterator<String> lines, Version version) {
        return new Lexer(
                lines,
                List.of(
                        new StringTokenConstructor(),
                        new NumberTokenConstructor(),
                        new KeywordTokenConstructor(keywords(version)),
                        new SymbolTokenConstructor(symbols(version))));
    }

    private static Map<String, TokenType> keywords(Version version) {
        Map<String, TokenType> keywords = new LinkedHashMap<>();
        keywords.put("let", TokenType.KEYWORD);
        keywords.put("println", TokenType.KEYWORD);
        keywords.put("number", TokenType.KEYWORD);
        keywords.put("string", TokenType.KEYWORD);
        keywords.putAll(keywordsAddedIn(version));
        return keywords;
    }

    // agregar una keyword de una versión es agregar una entrada acá
    private static Map<String, TokenType> keywordsAddedIn(Version version) {
        return switch (version) {
            case V10 -> Map.of();
            case V11 ->
                    Map.of(
                            "const", TokenType.KEYWORD,
                            "if", TokenType.KEYWORD,
                            "else", TokenType.KEYWORD,
                            "readInput", TokenType.KEYWORD,
                            "readEnv", TokenType.KEYWORD,
                            "boolean", TokenType.KEYWORD,
                            "true", TokenType.BOOLEAN,
                            "false", TokenType.BOOLEAN);
        };
    }

    private static Map<String, TokenType> symbols(Version version) {
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
        symbols.putAll(symbolsAddedIn(version));
        return symbols;
    }

    private static Map<String, TokenType> symbolsAddedIn(Version version) {
        return switch (version) {
            case V10 -> Map.of();
            case V11 -> Map.of("{", TokenType.PUNCTUATION, "}", TokenType.PUNCTUATION);
        };
    }
}
