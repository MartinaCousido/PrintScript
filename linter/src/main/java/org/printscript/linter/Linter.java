package org.printscript.linter;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.printscript.ast.Statement;
import org.printscript.common.MappedResults;
import org.printscript.common.Result;
import org.printscript.linter.rules.LintRule;

// por cada sentencia, corre todas las reglas y junta lo que reportan.
public final class Linter implements Iterator<Result<List<Violation>>> {

    private final List<LintRule> rules;
    private final Iterator<Result<List<Violation>>> checked;

    public Linter(Iterator<Result<Statement>> statements, List<LintRule> rules) {
        this.rules = List.copyOf(rules);
        this.checked =
                new MappedResults<>(Objects.requireNonNull(statements, "statements"), this::check);
    }

    @Override
    public boolean hasNext() {
        return checked.hasNext();
    }

    @Override
    public Result<List<Violation>> next() {
        return checked.next();
    }

    private List<Violation> check(Statement statement) {
        return rules.stream().flatMap(rule -> rule.check(statement).stream()).toList();
    }
}
