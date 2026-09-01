package org.printscript.linter.rules;

import java.util.List;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.PrintlnStatement;
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
            case Identifier _, NumberLiteral _, StringLiteral _ -> true;
            case BinaryExpression _ -> false;
        };
    }
}
