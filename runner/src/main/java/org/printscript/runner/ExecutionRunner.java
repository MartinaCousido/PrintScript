package org.printscript.runner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Version;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.InterpreterFactory;
import org.printscript.runtime.OutputPrinter;
import org.printscript.runtime.RuntimePorts;

// ejecuta
public final class ExecutionRunner {

    public List<Failure<?>> run(Iterator<String> lines, OutputPrinter printer) {
        return run(lines, printer, Version.V10);
    }

    public List<Failure<?>> run(Iterator<String> lines, OutputPrinter printer, Version version) {
        return run(
                lines,
                new RuntimePorts(printer, Optional::empty, name -> Optional.empty()),
                version);
    }

    /** Convenience API: retains all diagnostics; use the consumer overload for streaming. */
    public List<Failure<?>> run(Iterator<String> lines, RuntimePorts ports, Version version) {
        List<Failure<?>> failures = new ArrayList<>();
        run(lines, ports, version, failures::add);
        return failures;
    }

    /** Reports errors immediately; returns true when no diagnostic was produced. */
    public boolean run(
            Iterator<String> lines,
            RuntimePorts ports,
            Version version,
            Consumer<Failure<?>> report) {
        // arma lexer + buffer + parser + semantic: el Executor tiene como precondición
        // recibir sentencias ya validadas, y esta cadena es la que se lo garantiza
        Iterator<Result<Statement>> statements =
                StatementStreams.validatedStatementStream(lines, version);
        Interpreter interpreter = InterpreterFactory.create(statements, ports, version);
        return FailureCollector.consume(interpreter, report);
    }
}
