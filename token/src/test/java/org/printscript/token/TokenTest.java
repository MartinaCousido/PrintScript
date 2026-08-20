package org.printscript.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;

class TokenTest {

    private static Range anyRange() {
        return new Range(new Position(1, 1), new Position(1, 3));
    }

    @Test
    void keywordAndIdentifierShareTheSameTextButDifferInType() {
        String text = "let";
        Token keyword = new Token(TokenType.KEYWORD, text, anyRange());
        Token identifier = new Token(TokenType.IDENTIFIER, text, anyRange());

        assertEquals(text, keyword.value());
        assertEquals(text, identifier.value());

        assertEquals(TokenType.KEYWORD, keyword.type());
        assertEquals(TokenType.IDENTIFIER, identifier.type());
        assertNotEquals(keyword.type(), identifier.type());
    }

    @Test
    void tokenTypeExposesGenericCategoriesNotOnePerWord() {
        Set<String> categories =
                Arrays.stream(TokenType.values()).map(Enum::name).collect(Collectors.toSet());

        assertEquals(
                Set.of(
                        "KEYWORD",
                        "IDENTIFIER",
                        "NUMBER",
                        "STRING",
                        "OPERATOR",
                        "PUNCTUATION",
                        "BOOLEAN",
                        "UNKNOWN"),
                categories);

        assertFalse(categories.contains("LET"));
        assertFalse(categories.contains("PLUS"));
        assertFalse(categories.contains("SYMBOL"));
    }
}
