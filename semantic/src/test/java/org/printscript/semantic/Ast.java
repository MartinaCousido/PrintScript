package org.printscript.semantic;

import java.util.Optional;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Position;
import org.printscript.common.Range;

// arma nodos a mano para probar las reglas sin lexer ni parser
// cada nodo lleva un rango distinto para poder verificar a qué apunta cada error
final class Ast {

    static final Range ANY = at(9, 9);

    private Ast() {}

    static Range at(int line, int column) {
        Position position = new Position(line, column);
        return new Range(position, position);
    }

    static Identifier name(String value, Range range) {
        return new Identifier(value, range);
    }

    static Expression number(double value, Range range) {
        return new NumberLiteral(value, range);
    }

    static Expression text(String value, Range range) {
        return new StringLiteral(value, range);
    }

    static Expression binary(Expression left, Operator operator, Expression right) {
        return new BinaryExpression(left, operator, right, ANY);
    }

    static Statement declare(String variable, String type, Expression initializer) {
        return new VariableDeclaration(
                name(variable, ANY),
                new TypeName(type, ANY),
                Optional.ofNullable(initializer),
                ANY);
    }

    static Statement declare(
            String variable, Range variableRange, String type, Range typeRange, Expression init) {
        return new VariableDeclaration(
                name(variable, variableRange),
                new TypeName(type, typeRange),
                Optional.ofNullable(init),
                ANY);
    }

    static Expression bool(boolean value, Range range) {
        return new BooleanLiteral(value, range);
    }

    static Expression readInput(Expression prompt) {
        return new ReadInput(prompt, ANY);
    }

    static Expression readEnv(Expression name) {
        return new ReadEnv(name, ANY);
    }

    static Statement constant(String variable, String type, Expression initializer) {
        return new ConstDeclaration(
                name(variable, ANY),
                new TypeName(type, ANY),
                Optional.ofNullable(initializer),
                ANY);
    }

    static Statement assign(String variable, Range variableRange, Expression expression) {
        return new AssignmentStatement(name(variable, variableRange), expression, ANY);
    }

    static Statement println(Expression expression) {
        return new PrintlnStatement(expression, ANY);
    }
}
