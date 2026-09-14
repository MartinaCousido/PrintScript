package org.printscript.linter.rules;

import java.util.List;
import org.printscript.ast.Expression;
import org.printscript.ast.ReadInput;
import org.printscript.ast.Statement;
import org.printscript.linter.Violation;
import org.printscript.linter.visitors.ReadInputCollector;

// un identificador o un literal, nunca una expresión armada
public final class ReadInputArgumentRule implements LintRule {

    private final ReadInputCollector collector = new ReadInputCollector();

    @Override
    public List<Violation> check(Statement statement) {
        return statement.accept(collector).stream()
                .map(ReadInput::prompt)
                .filter(argument -> !SimpleArgument.test(argument))
                .map(ReadInputArgumentRule::violationFor)
                .toList();
    }

    private static Violation violationFor(Expression argument) {
        return new Violation(
                "readInput solo puede llamarse con un identificador o un literal",
                argument.range());
    }
}
