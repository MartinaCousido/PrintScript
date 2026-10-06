package org.printscript.linter.rules;

import org.printscript.ast.BinaryExpression;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.StringLiteral;

// qué cuenta como "un identificador o un literal".
final class SimpleArgument {

    private SimpleArgument() {}

    // Java 21 requires names for type patterns, even when only the type matters.
    @SuppressWarnings("PMD.UnusedLocalVariable")
    static boolean test(Expression expression) {
        return switch (expression) {
            // un literal booleano es un literal: println(true) es un argumento simple
            case Identifier identifier -> true;
            case NumberLiteral number -> true;
            case StringLiteral string -> true;
            case BooleanLiteral bool -> true;
            // una lectura externa es un llamado, no un identificador ni un literal
            case ReadInput input -> false;
            case ReadEnv env -> false;
            case BinaryExpression binary -> false;
        };
    }
}
