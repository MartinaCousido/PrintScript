package org.printscript.interpreter.providers;

import java.util.Set;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.interpreter.Environment;
import org.printscript.interpreter.Executor;
import org.printscript.interpreter.Interpreter;
import org.printscript.runtime.OutputPrinter;

public final class InterpreterProvider10 implements InterpreterProvider {

    private static final String VERSION = "1.0";

    @Override
    public Interpreter provide(PrintScriptIterator<Result<Statement>> statements, OutputPrinter printer) {
        Executor executor = new Executor(new Environment(), printer);
        return new Interpreter(statements, executor, supportedNodes(), VERSION);
    }

    private static Set<Class<? extends Statement>> supportedNodes() {
        return Set.of(
            VariableDeclaration.class,
            AssignmentStatement.class,
            PrintlnStatement.class);
    }
}
