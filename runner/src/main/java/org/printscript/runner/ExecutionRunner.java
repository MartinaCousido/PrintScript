package org.printscript.runner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Version;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.InterpreterFactory;
import org.printscript.runtime.OutputPrinter;
import org.printscript.runtime.RuntimePorts;

public final class ExecutionRunner {

    // la consigna define la versión como argumento opcional, con 1.0 por defecto
    public List<Failure<?>> run(Iterator<String> lines, OutputPrinter printer) {
        return run(lines, printer, Version.V10);
    }

    // sin puertos de entrada: un programa que use readInput o readEnv falla con un error
    // explícito, en vez de leer de algún lado que nadie eligió
    public List<Failure<?>> run(Iterator<String> lines, OutputPrinter printer, Version version) {
        return run(
                lines,
                new RuntimePorts(printer, Optional::empty, name -> Optional.empty()),
                version);
    }

    public List<Failure<?>> run(Iterator<String> lines, RuntimePorts ports, Version version) {
        // arma lexer + buffer + parser + semantic: el Executor tiene como precondición
        // recibir sentencias ya validadas, y esta cadena es la que se lo garantiza
        Iterator<Result<Statement>> statements =
                StatementStreams.validatedStatementStream(lines, version);
        Interpreter interpreter = InterpreterFactory.create(statements, ports, version);
        List<Failure<?>> failures = new ArrayList<>();
        while (interpreter.hasNext()) {
            Result<?> result = interpreter.next();
            if (result instanceof Failure<?> failure) {
                failures.add(failure);
            }
        }
        return failures;
    }
}
