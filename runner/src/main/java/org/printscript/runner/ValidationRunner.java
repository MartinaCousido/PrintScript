package org.printscript.runner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;

public final class ValidationRunner {

    // valida sintaxis y semántica sin ejecutar nada, como pide la consigna
    public List<Failure<?>> validate(Iterator<String> lines) {
        PrintScriptIterator<Result<Statement>> statements =
                StatementStreams.validatedStatementStream(lines);
        List<Failure<?>> failures = new ArrayList<>();
        while (statements.hasNext()) {
            Result<?> result = statements.getNext();
            if (result instanceof Failure<?> failure) {
                failures.add(failure);
            }
        }
        return failures;
    }
}
