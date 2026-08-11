package org.printscript.ast;

import org.printscript.common.Range;

public sealed interface Expression permits NumberLiteral, StringLiteral, Identifier, BinaryExpression {

    Range range();
}
