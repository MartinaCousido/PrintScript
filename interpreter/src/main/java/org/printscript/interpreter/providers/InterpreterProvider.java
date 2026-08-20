package org.printscript.interpreter.providers;

import org.printscript.ast.Statement;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.interpreter.Interpreter;
import org.printscript.runtime.OutputPrinter;

@FunctionalInterface
public interface InterpreterProvider {

    Interpreter provide(PrintScriptIterator<Result<Statement>> statements, OutputPrinter printer);
}
