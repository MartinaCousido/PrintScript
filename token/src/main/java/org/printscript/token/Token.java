package org.printscript.token;

import org.printscript.common.Range;

public record Token(TokenType type, String value, Range range) {
}
