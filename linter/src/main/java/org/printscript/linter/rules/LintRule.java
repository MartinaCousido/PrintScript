package org.printscript.linter.rules;

import java.util.List;
import org.printscript.ast.Statement;
import org.printscript.linter.Violation;

@FunctionalInterface
public interface LintRule {

    List<Violation> check(Statement statement);
}
