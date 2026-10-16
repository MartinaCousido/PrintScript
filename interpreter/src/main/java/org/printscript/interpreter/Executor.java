package org.printscript.interpreter;

import java.util.List;
import java.util.Optional;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Expression;
import org.printscript.ast.IfStatement;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.ScopedStatementVisitor;
import org.printscript.ast.Statement;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.runtime.RuntimePorts;

// ejecuta las sentencias
public final class Executor extends ScopedStatementVisitor {

    private final RuntimePorts ports;
    private Environment environment;

    public Executor(Environment environment, RuntimePorts ports) {
        this.environment = environment;
        this.ports = ports;
    }

    @Override
    public Result<Void> visitVariableDeclaration(VariableDeclaration variableDeclaration) {
        return declare(
                variableDeclaration.name().name(),
                variableDeclaration.type().name(),
                variableDeclaration.initializer(),
                variableDeclaration.range(),
                false);
    }

    @Override
    public Result<Void> visitConstDeclaration(ConstDeclaration constDeclaration) {
        return declare(
                constDeclaration.name().name(),
                constDeclaration.type().name(),
                constDeclaration.initializer(),
                constDeclaration.range(),
                true);
    }

    @Override
    public Result<Void> visitAssignmentStatement(AssignmentStatement assignmentStatement) {
        return evaluate(assignmentStatement.expression())
                .andThen(value -> assign(value, assignmentStatement));
    }

    private Result<Void> assign(Object value, AssignmentStatement assignmentStatement) {
        String name = assignmentStatement.name().name();
        Optional<Variable> existing = environment.read(name);
        if (existing.isEmpty()) {
            return new Failure<>("Variable no declarada: " + name, assignmentStatement.range());
        }
        // la conversión se aplica siempre; para cualquier inicializador que no sea una
        // lectura externa es un no-op, porque semantic ya garantizó que el tipo coincide
        return Conversions.toDeclaredType(
                        existing.get().declaredType(), value, assignmentStatement.range())
                .andThen(converted -> store(name, converted, assignmentStatement));
    }

    private Result<Void> store(String name, Object value, AssignmentStatement assignmentStatement) {
        return environment
                .assign(name, value)
                .<Result<Void>>map(this::commit)
                .orElseGet(() -> rejectedAssignment(assignmentStatement));
    }

    @Override
    protected void openScope() {
        environment = environment.openScope();
    }

    @Override
    protected void closeScope() {
        environment = environment.closeScope();
    }

    @Override
    protected Result<List<Statement>> branches(IfStatement ifStatement) {
        return evaluate(ifStatement.condition())
                .andThen(
                        value -> {
                            if (!(value instanceof Boolean)) {
                                return new Failure<>(
                                        "La condición de un if tiene que ser boolean",
                                        ifStatement.condition().range());
                            }
                            return new Success<>(
                                    (Boolean) value
                                            ? List.of(ifStatement.thenBlock())
                                            : ifStatement
                                                    .elseBranch()
                                                    .map(List::of)
                                                    .orElseGet(List::of));
                        });
    }

    @Override
    public Result<Void> visitPrintlnStatement(PrintlnStatement printlnStatement) {
        return evaluate(printlnStatement.expression())
                .map(
                        value -> {
                            ports.printer().println(ExpressionEvaluator.asText(value));
                            return null;
                        });
    }

    private Result<Object> evaluate(Expression expression) {
        return expression.accept(new ExpressionEvaluator(environment, ports));
    }

    // evalúa el inicializador si lo hay y guarda la variable
    private Result<Void> declare(
            String name,
            String declaredType,
            Optional<Expression> initializer,
            Range range,
            boolean immutable) {
        if (initializer.isEmpty()) {
            return declareValue(name, declaredType, null, range, immutable);
        }
        return evaluate(initializer.get())
                .andThen(value -> Conversions.toDeclaredType(declaredType, value, range))
                .andThen(value -> declareValue(name, declaredType, value, range, immutable));
    }

    private Result<Void> declareValue(
            String name, String declaredType, Object value, Range range, boolean immutable) {
        return environment
                .declare(name, declaredType, value, immutable)
                .<Result<Void>>map(this::commit)
                .orElseGet(() -> new Failure<>("Variable ya declarada: " + name, range));
    }

    // assign devuelve vacío por dos motivos distintos y el mensaje tiene que decir cuál
    private Result<Void> rejectedAssignment(AssignmentStatement assignment) {
        String name = assignment.name().name();
        if (environment.read(name).isPresent()) {
            return new Failure<>("No se puede reasignar la constante " + name, assignment.range());
        }
        return new Failure<>("Variable no declarada: " + name, assignment.range());
    }

    private Result<Void> commit(Environment updated) {
        this.environment = updated;
        return new Success<>(null);
    }

    @Override
    protected Result<Void> notAStatement(Range range) {
        return new Failure<>("No es una sentencia ejecutable", range);
    }
}
