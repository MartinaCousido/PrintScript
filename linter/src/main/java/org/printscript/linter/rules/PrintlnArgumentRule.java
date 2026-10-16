package org.printscript.linter.rules;

import java.util.List;
import org.printscript.ast.Expression;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.linter.Violation;
import org.printscript.linter.config.LinterConfiguration;
import org.printscript.linter.visitors.PrintlnCollector;

// println recibe un literal o un identificador, no una expresión
public final class PrintlnArgumentRule implements LintRule {

    private final PrintlnCollector collector = new PrintlnCollector();

    // recorre para encontrar también los println de adentro de un bloque, y reporta
    // todos los incumplimientos
    @Override
    public List<Violation> check(Statement statement) {
        return statement.accept(collector).stream()
                .map(PrintlnStatement::expression)
                .filter(argument -> !SimpleArgument.test(argument))
                .map(PrintlnArgumentRule::violationFor)
                .toList();
    }

    private static Violation violationFor(Expression argument) {
        return new Violation(
                LinterConfiguration.PRINTLN_ARGUMENT_KEY,
                "println solo puede llamarse con un identificador o un literal",
                argument.range());
    }
}
