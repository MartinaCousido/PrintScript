package org.printscript.linter;

import java.util.ArrayList;
import java.util.List;
import org.printscript.ast.Statement;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.linter.config.IdentifierFormat;
import org.printscript.linter.config.LinterConfiguration;
import org.printscript.linter.rules.IdentifierFormatRule;
import org.printscript.linter.rules.LintRule;
import org.printscript.linter.rules.PrintlnArgumentRule;
import org.printscript.linter.rules.format.CamelCaseChecker;
import org.printscript.linter.rules.format.IdentifierFormatChecker;

public final class LinterFactory {

    private LinterFactory() {}

    public static Linter create(
            PrintScriptIterator<Result<Statement>> statements, LinterConfiguration configuration) {
        return new Linter(statements, rulesFor(configuration));
    }

    private static List<LintRule> rulesFor(LinterConfiguration configuration) {
        List<LintRule> rules = new ArrayList<>();
        configuration
                .identifierFormat()
                .map(LinterFactory::checkerFor)
                .map(IdentifierFormatRule::new)
                .ifPresent(rules::add);
        if (configuration.printlnOnlySimpleArgument()) {
            rules.add(new PrintlnArgumentRule());
        }
        return List.copyOf(rules);
    }

    private static IdentifierFormatChecker checkerFor(IdentifierFormat format) {
        return switch (format) {
            case CAMEL_CASE -> new CamelCaseChecker();
        };
    }
}
