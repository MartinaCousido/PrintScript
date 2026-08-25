package org.printscript.runner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.InterpreterFactory;
import org.printscript.parser.Parser;
import org.printscript.runtime.OutputPrinter;

public final class ExecutionRunner {

    public List<Failure<?>> run(Iterator<String> lines, OutputPrinter printer) {
        Parser parser = StatementStreams.statementStream(lines);
        Interpreter interpreter = InterpreterFactory.create(parser, printer);
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
