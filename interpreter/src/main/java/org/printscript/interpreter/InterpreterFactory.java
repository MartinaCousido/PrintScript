package org.printscript.interpreter;

import java.util.Set;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.runtime.OutputPrinter;

public final class InterpreterFactory {

    private InterpreterFactory() {}

    public static Interpreter create(
            PrintScriptIterator<Result<Statement>> statements, OutputPrinter printer) {
        Executor executor = new Executor(new Environment(), printer);
        return new Interpreter(statements, executor, supportedNodes());
    }

    private static Set<Class<? extends Statement>> supportedNodes() {
        return Set.of(VariableDeclaration.class, AssignmentStatement.class, PrintlnStatement.class);
    }
}
