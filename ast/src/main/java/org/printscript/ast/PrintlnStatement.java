package org.printscript.ast;

import org.printscript.common.Range;

public record PrintlnStatement(Expression expression, Range range) implements Statement {
}
