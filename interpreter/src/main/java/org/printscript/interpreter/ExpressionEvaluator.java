package org.printscript.interpreter;

import java.math.BigDecimal;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.ExpressionOnlyVisitor;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.StringLiteral;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.runtime.RuntimePorts;

// calcula cuánto vale una expresión, recorriendo su árbol
// calcula valores
public final class ExpressionEvaluator extends ExpressionOnlyVisitor<Result<Object>> {

    private final Environment environment;
    private final RuntimePorts ports;

    public ExpressionEvaluator(Environment environment, RuntimePorts ports) {
        this.environment = environment;
        this.ports = ports;
    }

    @Override
    public Result<Object> visitNumberLiteral(NumberLiteral numberLiteral) {
        return new Success<>(numberLiteral.value());
    }

    @Override
    public Result<Object> visitStringLiteral(StringLiteral stringLiteral) {
        return new Success<>(stringLiteral.value());
    }

    @Override
    public Result<Object> visitBooleanLiteral(BooleanLiteral booleanLiteral) {
        return new Success<>(booleanLiteral.value());
    }

    // el prompt se escribe sin salto de línea, para que quede en la misma línea que lo
    // que el usuario tipea
    @Override
    public Result<Object> visitReadInput(ReadInput readInput) {
        Result<Object> prompt = readInput.prompt().accept(this);
        if (prompt instanceof Failure<Object> failure) {
            return failure;
        }
        ports.printer().print(asText(((Success<Object>) prompt).value()));
        return ports.input()
                .readLine()
                .<Result<Object>>map(Success::new)
                .orElseGet(
                        () ->
                                new Failure<>(
                                        "No hay más entrada disponible para readInput",
                                        readInput.range()));
    }

    @Override
    public Result<Object> visitReadEnv(ReadEnv readEnv) {
        Result<Object> name = readEnv.name().accept(this);
        if (name instanceof Failure<Object> failure) {
            return failure;
        }
        String variable = asText(((Success<Object>) name).value());
        return ports.env()
                .read(variable)
                .<Result<Object>>map(Success::new)
                .orElseGet(
                        () ->
                                new Failure<>(
                                        "La variable de entorno " + variable + " no está definida",
                                        readEnv.range()));
    }

    @Override
    public Result<Object> visitIdentifier(Identifier identifier) {
        return environment
                .read(identifier.name())
                .<Result<Object>>map(variable -> new Success<>(variable.value()))
                .orElseGet(
                        () ->
                                new Failure<>(
                                        "Variable no declarada: " + identifier.name(),
                                        identifier.range()));
    }

    @Override
    public Result<Object> visitBinaryExpression(BinaryExpression binaryExpression) {
        Result<Object> left = binaryExpression.left().accept(this);
        if (left instanceof Failure<Object> failure) {
            return failure;
        }
        Result<Object> right = binaryExpression.right().accept(this);
        if (right instanceof Failure<Object> failure) {
            return failure;
        }
        return combine(
                ((Success<Object>) left).value(),
                binaryExpression.operator(),
                ((Success<Object>) right).value(),
                binaryExpression.range());
    }

    // pasa un número a texto
    static String asText(Object value) {
        if (value instanceof Double number && Double.isFinite(number)) {
            return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private static Result<Object> combine(
            Object left, Operator operator, Object right, Range range) {
        if (left instanceof Double leftNumber && right instanceof Double rightNumber) {
            return new Success<>(arithmetic(leftNumber, operator, rightNumber));
        }
        if (operator == Operator.PLUS && (left instanceof String || right instanceof String)) {
            return new Success<>(asText(left) + asText(right));
        }
        return new Failure<>(
                "No se puede aplicar "
                        + operator
                        + " entre "
                        + typeOf(left)
                        + " y "
                        + typeOf(right),
                range);
    }

    private static double arithmetic(double left, Operator operator, double right) {
        return switch (operator) {
            case PLUS -> left + right;
            case MINUS -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> left / right;
        };
    }

    private static String typeOf(Object value) {
        if (value instanceof String) {
            return "string";
        }
        return value instanceof Boolean ? "boolean" : "number";
    }

    @Override
    protected Result<Object> notAnExpression(Range range) {
        return new Failure<>("No es una expresión evaluable", range);
    }
}
