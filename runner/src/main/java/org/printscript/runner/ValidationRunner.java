package org.printscript.runner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Version;

// valida sin ejecutar
public final class ValidationRunner {

    public List<Failure<?>> validate(Iterator<String> lines) {
        return validate(lines, Version.V10);
    }

    // valida sintaxis y semántica sin ejecutar nada
    /** Convenience API: retains all diagnostics; use the consumer overload for streaming. */
    public List<Failure<?>> validate(Iterator<String> lines, Version version) {
        List<Failure<?>> failures = new ArrayList<>();
        validate(lines, version, failures::add);
        return failures;
    }

    /** Reports errors immediately; returns true when no diagnostic was produced. */
    public boolean validate(Iterator<String> lines, Version version, Consumer<Failure<?>> report) {
        Iterator<Result<Statement>> statements =
                StatementStreams.validatedStatementStream(lines, version);
        return FailureCollector.consume(statements, report);
    }
}
