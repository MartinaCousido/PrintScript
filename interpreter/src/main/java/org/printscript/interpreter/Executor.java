package org.printscript.interpreter;

import java.util.Optional;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.Block;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Expression;
import org.printscript.ast.IfStatement;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.StatementOnlyVisitor;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.runtime.OutputPrinter;

// ejecuta las sentencias
public final class Executor extends StatementOnlyVisitor<Result<Void>> {

    private final OutputPrinter printer;
    private Environment environment;

    public Executor(Environment environment, OutputPrinter printer) {
        this.environment = environment;
        this.printer = printer;
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
        Result<Object> evaluated = evaluate(assignmentStatement.expression());
        if (evaluated instanceof Failure<Object> failure) {
            return propagate(failure);
        }
        return environment
                .assign(assignmentStatement.name().name(), valueOf(evaluated))
                .<Result<Void>>map(this::commit)
                .orElseGet(() -> rejectedAssignment(assignmentStatement));
    }

    @Override
    public Result<Void> visitBlock(Block block) {
        return notSupportedYet(block.range());
    }

    @Override
    public Result<Void> visitIfStatement(IfStatement ifStatement) {
        return notSupportedYet(ifStatement.range());
    }

    // el bloque y el if todavía no se ejecutan; el error viaja como dato, como
    // cualquier otro, en vez de romper con una excepción
    private static Result<Void> notSupportedYet(Range range) {
        return new Failure<>("El intérprete todavía no ejecuta bloques ni if", range);
    }

    @Override
    public Result<Void> visitPrintlnStatement(PrintlnStatement printlnStatement) {
        Result<Object> evaluated = evaluate(printlnStatement.expression());
        if (evaluated instanceof Failure<Object> failure) {
            return propagate(failure);
        }
        printer.print(ExpressionEvaluator.asText(valueOf(evaluated)));
        return new Success<>(null);
    }

    private Result<Object> evaluate(Expression expression) {
        return expression.accept(new ExpressionEvaluator(environment));
    }

    // evalúa el inicializador si lo hay y guarda la variable
    private Result<Void> declare(
            String name,
            String declaredType,
            Optional<Expression> initializer,
            Range range,
            boolean immutable) {
        Object value = null;
        if (initializer.isPresent()) {
            Result<Object> evaluated = evaluate(initializer.get());
            if (evaluated instanceof Failure<Object> failure) {
                return propagate(failure);
            }
            value = valueOf(evaluated);
        }
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

    private static Object valueOf(Result<Object> result) {
        return ((Success<Object>) result).value();
    }

    private static Result<Void> propagate(Failure<Object> failure) {
        return new Failure<>(failure.message(), failure.range());
    }

    @Override
    protected Result<Void> notAStatement(Range range) {
        return new Failure<>("No es una sentencia ejecutable", range);
    }
}
