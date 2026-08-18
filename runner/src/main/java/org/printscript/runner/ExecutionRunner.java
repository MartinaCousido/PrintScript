package org.printscript.runner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.providers.InterpreterVersionProvider;
import org.printscript.lexer.Lexer;
import org.printscript.lexer.providers.LexerVersionProvider;
import org.printscript.parser.Parser;
import org.printscript.parser.ParserVersionProvider;
import org.printscript.parser.TokenBuffer;
import org.printscript.runtime.OutputPrinter;

public final class ExecutionRunner {

    public List<Failure<?>> run(Iterator<String> lines, String version, OutputPrinter printer) {
        Parser parser = statementStream(lines, version);
        Interpreter interpreter = new InterpreterVersionProvider().providerFor(version).provide(parser, printer);
        List<Failure<?>> failures = new ArrayList<>();
        while (interpreter.hasNext()) {
            Result<?> result = interpreter.getNext();
            if (result instanceof Failure<?> failure) {
                failures.add(failure);
            }
        }
        return failures;
    }

    private Parser statementStream(Iterator<String> lines, String version) {
        Lexer lexer = new LexerVersionProvider().providerFor(version).provide(lines);
        TokenBuffer buffer = new TokenBuffer(lexer);
        return new ParserVersionProvider().providerFor(version).provide(buffer);
    }
}
