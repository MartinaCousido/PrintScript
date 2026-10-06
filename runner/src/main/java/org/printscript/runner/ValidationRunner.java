package org.printscript.runner;

import java.util.Iterator;
import java.util.List;
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
    public List<Failure<?>> validate(Iterator<String> lines, Version version) {
        Iterator<Result<Statement>> statements =
                StatementStreams.validatedStatementStream(lines, version);
        return FailureCollector.collect(statements);
    }
}
