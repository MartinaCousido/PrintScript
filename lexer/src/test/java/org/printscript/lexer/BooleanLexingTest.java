package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Version;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class BooleanLexingTest {

    private static List<String> categories(Version version, String line) {
        Lexer lexer = LexerFactory.create(List.of(line).iterator(), version);
        List<String> described = new ArrayList<>();
        while (lexer.hasNext()) {
            Token token = lexer.getNext();
            described.add(token.type() + "|" + token.value());
        }
        return described;
    }

    @Test
    void inVersion11TrueAndFalseAreBooleanLiterals() {
        assertEquals(
                List.of(TokenType.BOOLEAN + "|true", TokenType.BOOLEAN + "|false"),
                categories(Version.V11, "true false"));
    }

    @Test
    void inVersion11BooleanIsATypeAnnotation() {
        assertEquals(List.of(TokenType.KEYWORD + "|boolean"), categories(Version.V11, "boolean"));
    }

    @Test
    void inVersion10TheyAreOrdinaryIdentifiers() {
        // 1.0 no tiene booleanos: las tres palabras son nombres como cualquier otro
        assertEquals(
                List.of(
                        TokenType.IDENTIFIER + "|true",
                        TokenType.IDENTIFIER + "|false",
                        TokenType.IDENTIFIER + "|boolean"),
                categories(Version.V10, "true false boolean"));
    }

    @Test
    void aWordThatStartsWithTrueIsAnIdentifierAndNotALiteral() {
        // el constructor consume la palabra entera antes de buscarla en la tabla, así
        // que el maximal munch sale solo: no hay un "true" seguido de un "Value"
        assertEquals(
                List.of(TokenType.IDENTIFIER + "|trueValue"), categories(Version.V11, "trueValue"));
        assertEquals(
                List.of(TokenType.IDENTIFIER + "|falsehood"), categories(Version.V11, "falsehood"));
        assertEquals(List.of(TokenType.IDENTIFIER + "|true_"), categories(Version.V11, "true_"));
    }
}
