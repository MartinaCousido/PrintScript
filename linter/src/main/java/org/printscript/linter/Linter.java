package org.printscript.linter;

import java.util.List;
import org.printscript.ast.Statement;
import org.printscript.common.MappedResults;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.linter.rules.LintRule;

public final class Linter implements PrintScriptIterator<Result<List<Violation>>> {

    private final List<LintRule> rules;
    private final PrintScriptIterator<Result<List<Violation>>> checked;

    public Linter(PrintScriptIterator<Result<Statement>> statements, List<LintRule> rules) {
        this.rules = List.copyOf(rules);
        this.checked = new MappedResults<>(statements, this::check);
    }

    @Override
    public boolean hasNext() {
        return checked.hasNext();
    }

    @Override
    public Result<List<Violation>> getNext() {
        return checked.getNext();
    }

    private List<Violation> check(Statement statement) {
        return rules.stream().flatMap(rule -> rule.check(statement).stream()).toList();
    }
}
