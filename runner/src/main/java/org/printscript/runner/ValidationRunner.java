package org.printscript.runner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Version;

public final class ValidationRunner {

    // la consigna define la versión como argumento opcional, con 1.0 por defecto
    public List<Failure<?>> validate(Iterator<String> lines) {
        return validate(lines, Version.V10);
    }

    // valida sintaxis y semántica sin ejecutar nada, como pide la consigna
    public List<Failure<?>> validate(Iterator<String> lines, Version version) {
        Iterator<Result<Statement>> statements =
                StatementStreams.validatedStatementStream(lines, version);
        List<Failure<?>> failures = new ArrayList<>();
        while (statements.hasNext()) {
            Result<?> result = statements.next();
            if (result instanceof Failure<?> failure) {
                failures.add(failure);
            }
        }
        return failures;
    }
}
