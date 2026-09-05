package org.printscript.ast;

import org.printscript.common.Range;

// algo que vale algo.
public sealed interface Expression
        permits NumberLiteral,
                StringLiteral,
                BooleanLiteral,
                Identifier,
                BinaryExpression,
                ReadInput,
                ReadEnv {

    Range range();

    <T> T accept(ASTVisitor<T> visitor);
}
