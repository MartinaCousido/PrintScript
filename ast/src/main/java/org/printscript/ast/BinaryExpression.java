package org.printscript.ast;

import org.printscript.common.Range;

public record BinaryExpression(Expression left, Operator operator, Expression right, Range range) implements Expression {
}
