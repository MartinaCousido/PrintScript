package org.printscript.runner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.InterpreterFactory;
import org.printscript.runtime.OutputPrinter;

public final class ExecutionRunner {

    public List<Failure<?>> run(Iterator<String> lines, OutputPrinter printer) {
        // arma lexer + buffer + parser + semantic: el Executor tiene como precondición
        // recibir sentencias ya validadas, y esta cadena es la que se lo garantiza
        PrintScriptIterator<Result<Statement>> statements =
                StatementStreams.validatedStatementStream(lines);
        Interpreter interpreter = InterpreterFactory.create(statements, printer);
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
