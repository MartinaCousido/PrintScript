package org.printscript.runner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.common.Version;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.InterpreterFactory;
import org.printscript.runtime.OutputPrinter;

public final class ExecutionRunner {

    // la consigna define la versión como argumento opcional, con 1.0 por defecto
    public List<Failure<?>> run(Iterator<String> lines, OutputPrinter printer) {
        return run(lines, printer, Version.V10);
    }

    public List<Failure<?>> run(Iterator<String> lines, OutputPrinter printer, Version version) {
        // arma lexer + buffer + parser + semantic: el Executor tiene como precondición
        // recibir sentencias ya validadas, y esta cadena es la que se lo garantiza
        PrintScriptIterator<Result<Statement>> statements =
                StatementStreams.validatedStatementStream(lines, version);
        Interpreter interpreter = InterpreterFactory.create(statements, printer, version);
        List<Failure<?>> failures = new ArrayList<>();
        while (interpreter.hasNext()) {
            Result<?> result = interpreter.getNext();
            if (result instanceof Failure<?> failure) {
                failures.add(failure);
            }
        }
        return failures;
    }
}
