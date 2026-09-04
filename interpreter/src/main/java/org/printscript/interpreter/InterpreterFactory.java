package org.printscript.interpreter;

import java.util.Set;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.common.Version;
import org.printscript.runtime.OutputPrinter;

// acá vive todo lo que el intérprete sabe de cada versión: qué construcciones puede
// ejecutar. Es la compuerta que hace que un programa 1.1 corrido como 1.0 falle con un
// mensaje explícito en vez de comportarse raro.
public final class InterpreterFactory {

    private InterpreterFactory() {}

    public static Interpreter create(
            PrintScriptIterator<Result<Statement>> statements,
            OutputPrinter printer,
            Version version) {
        Executor executor = new Executor(new Environment(), printer);
        return new Interpreter(statements, executor, supportedNodes(version));
    }

    private static Set<Class<? extends Statement>> supportedNodes(Version version) {
        Set<Class<? extends Statement>> common =
                Set.of(
                        VariableDeclaration.class,
                        AssignmentStatement.class,
                        PrintlnStatement.class);
        return switch (version) {
            // 1.1 suma ConstDeclaration e IfStatement cuando esos nodos existan
            case V10, V11 -> common;
        };
    }
}
