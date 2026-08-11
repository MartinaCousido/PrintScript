package org.printscript.token;

import org.printscript.common.Range;
//una pieza concreta: su categoría, su texto y su range
public record Token(TokenType type, String value, Range range) {
}
