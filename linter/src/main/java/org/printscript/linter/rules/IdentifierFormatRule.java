package org.printscript.linter.rules;

import java.util.List;
import org.printscript.ast.Identifier;
import org.printscript.ast.Statement;
import org.printscript.linter.Violation;
import org.printscript.linter.rules.format.IdentifierFormatChecker;
import org.printscript.linter.visitors.IdentifierCollector;

public final class IdentifierFormatRule implements LintRule {

    private final IdentifierFormatChecker checker;
    private final IdentifierCollector collector = new IdentifierCollector();

    public IdentifierFormatRule(IdentifierFormatChecker checker) {
        this.checker = checker;
    }

    @Override
    public List<Violation> check(Statement statement) {
        return statement.accept(collector).stream()
                .filter(identifier -> !checker.isValid(identifier.name()))
                .map(this::violationFor)
                .toList();
    }

    private Violation violationFor(Identifier identifier) {
        return new Violation(
                "El identificador '"
                        + identifier.name()
                        + "' debe estar escrito en "
                        + checker.expectedFormat(),
                identifier.range());
    }
}
