package org.printscript.linter;

import java.util.List;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.linter.rules.LintRule;

public final class Linter implements PrintScriptIterator<Result<List<Violation>>> {

    private final PrintScriptIterator<Result<Statement>> statements;
    private final List<LintRule> rules;

    public Linter(PrintScriptIterator<Result<Statement>> statements, List<LintRule> rules) {
        this.statements = statements;
        this.rules = List.copyOf(rules);
    }

    @Override
    public boolean hasNext() {
        return statements.hasNext();
    }

    @Override
    public Result<List<Violation>> getNext() {
        Result<Statement> next = statements.getNext();
        if (next instanceof Failure<Statement> failure) {
            return new Failure<>(failure.message(), failure.range());
        }
        Statement statement = ((Success<Statement>) next).value();
        return new Success<>(
                rules.stream().flatMap(rule -> rule.check(statement).stream()).toList());
    }
}
