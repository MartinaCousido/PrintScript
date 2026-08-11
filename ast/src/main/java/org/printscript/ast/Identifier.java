package org.printscript.ast;

import org.printscript.common.Range;

public record Identifier(String name, Range range) implements Expression {
}
