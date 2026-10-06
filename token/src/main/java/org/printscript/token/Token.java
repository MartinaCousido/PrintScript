package org.printscript.token;

import java.util.Objects;
import org.printscript.common.Range;

// una pieza concreta: su categoría, su texto y su range
public record Token(TokenType type, String value, Range range) {

    public Token {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(range, "range");
    }
}
