package org.printscript.linter.rules;

import java.util.List;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.linter.Violation;

public final class PrintlnArgumentRule implements LintRule {

    @Override
    public List<Violation> check(Statement statement) {
        if (!(statement instanceof PrintlnStatement println)) {
            return List.of();
        }
        Expression argument = println.expression();
        if (isSimple(argument)) {
            return List.of();
        }
        return List.of(
                new Violation(
                        "println solo puede llamarse con un identificador o un literal",
                        argument.range()));
    }

    private static boolean isSimple(Expression expression) {
        return switch (expression) {
            // un literal booleano es un literal: println(true) es un argumento simple
            case Identifier _, NumberLiteral _, StringLiteral _, BooleanLiteral _ -> true;
            // una lectura externa es un llamado, no un identificador ni un literal
            case ReadInput _, ReadEnv _ -> false;
            case BinaryExpression _ -> false;
        };
    }
}
