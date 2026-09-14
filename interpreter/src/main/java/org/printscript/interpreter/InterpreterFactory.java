package org.printscript.interpreter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.IfStatement;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Result;
import org.printscript.common.Version;
import org.printscript.runtime.RuntimePorts;

// acá vive lo que el intérprete sabe de cada versión
public final class InterpreterFactory {

    private InterpreterFactory() {}

    public static Interpreter create(
            Iterator<Result<Statement>> statements, RuntimePorts ports, Version version) {
        Executor executor = new Executor(new Environment(), ports);
        return new Interpreter(statements, executor, supportedNodes(version));
    }

    @SafeVarargs
    private static Set<Class<? extends Statement>> union(
            Set<Class<? extends Statement>> base, Class<? extends Statement>... extra) {
        Set<Class<? extends Statement>> all = new HashSet<>(base);
        all.addAll(Set.of(extra));
        return Set.copyOf(all);
    }

    private static Set<Class<? extends Statement>> supportedNodes(Version version) {
        Set<Class<? extends Statement>> common =
                Set.of(
                        VariableDeclaration.class,
                        AssignmentStatement.class,
                        PrintlnStatement.class);
        return switch (version) {
            case V10 -> common;
            case V11 -> union(common, ConstDeclaration.class, IfStatement.class);
        };
    }
}
