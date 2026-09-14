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

    static boolean test(Expression expression) {
        return switch (expression) {
            // un literal booleano es un literal: println(true) es un argumento simple
            case Identifier _, NumberLiteral _, StringLiteral _, BooleanLiteral _ -> true;
            // una lectura externa es un llamado, no un identificador ni un literal
            case ReadInput _, ReadEnv _ -> false;
            case BinaryExpression _ -> false;
        };
    }
}
