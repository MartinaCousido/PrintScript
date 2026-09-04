package org.printscript.semantic;

import org.printscript.ast.BinaryExpression;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.Expression;
import org.printscript.ast.ExpressionOnlyVisitor;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.StringLiteral;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;

// calcula qué tipo tiene una expresión, sin evaluarla
// mira la tabla de tipos pero no la modifica: declarar es cosa de las sentencias
final class ExpressionTyper extends ExpressionOnlyVisitor<Result<Type>> {

    private final SymbolTable symbols;

    ExpressionTyper(SymbolTable symbols) {
        this.symbols = symbols;
    }

    @Override
    public Result<Type> visitNumberLiteral(NumberLiteral numberLiteral) {
        return new Success<>(Type.NUMBER);
    }

    @Override
    public Result<Type> visitStringLiteral(StringLiteral stringLiteral) {
        return new Success<>(Type.STRING);
    }

    @Override
    public Result<Type> visitBooleanLiteral(BooleanLiteral booleanLiteral) {
        return new Success<>(Type.BOOLEAN);
    }

    @Override
    public Result<Type> visitIdentifier(Identifier identifier) {
        return symbols.read(identifier.name())
                .map(Symbol::type)
                .<Result<Type>>map(Success::new)
                .orElseGet(
                        () ->
                                new Failure<>(
                                        "Variable no declarada: " + identifier.name(),
                                        identifier.range()));
    }

    @Override
    public Result<Type> visitBinaryExpression(BinaryExpression binaryExpression) {
        Result<Type> left = binaryExpression.left().accept(this);
        if (left instanceof Failure<Type> failure) {
            return failure;
        }
        Result<Type> right = binaryExpression.right().accept(this);
        if (right instanceof Failure<Type> failure) {
            return failure;
        }
        return combine(
                ((Success<Type>) left).value(),
                binaryExpression.operator(),
                ((Success<Type>) right).value(),
                binaryExpression);
    }

    // la concatenación es el único caso donde los dos lados pueden tener tipos distintos
    // un boolean no participa de ninguna: ni se suma, ni se resta, ni se concatena
    private static Result<Type> combine(
            Type left, Operator operator, Type right, BinaryExpression expression) {
        if (left == Type.BOOLEAN || right == Type.BOOLEAN) {
            return arithmeticFailure(operator, left, right, expression.range());
        }
        if (operator == Operator.PLUS) {
            boolean anyString = left == Type.STRING || right == Type.STRING;
            return new Success<>(anyString ? Type.STRING : Type.NUMBER);
        }
        if (left != Type.NUMBER || right != Type.NUMBER) {
            return arithmeticFailure(operator, left, right, expression.range());
        }
        return new Success<>(Type.NUMBER);
    }

    // mismo mensaje y mismo rango que usaba el intérprete para este error: la etapa
    // cambia, pero lo que ve el usuario tiene que seguir siendo lo mismo
    private static Result<Type> arithmeticFailure(
            Operator operator, Type left, Type right, Range range) {
        return new Failure<>(
                "No se puede aplicar "
                        + operator
                        + " entre "
                        + left.writtenName()
                        + " y "
                        + right.writtenName(),
                range);
    }

    @Override
    protected Result<Type> notAnExpression(Range range) {
        return new Failure<>("No es una expresión evaluable", range);
    }

    static Result<Type> typeOf(Expression expression, SymbolTable symbols) {
        return expression.accept(new ExpressionTyper(symbols));
    }
}
