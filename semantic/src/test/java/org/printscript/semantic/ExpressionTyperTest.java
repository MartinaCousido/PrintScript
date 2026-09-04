package org.printscript.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.printscript.semantic.Ast.assign;
import static org.printscript.semantic.Ast.at;
import static org.printscript.semantic.Ast.declare;
import static org.printscript.semantic.Ast.number;
import static org.printscript.semantic.Ast.println;

import org.junit.jupiter.api.Test;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Range;

class ExpressionTyperTest {

    private static final String NOT_AN_EXPRESSION = "No es una expresión evaluable";

    private final ExpressionTyper typer = new ExpressionTyper(new SymbolTable());

    private Failure<Type> visit(Statement statement) {
        return assertInstanceOf(Failure.class, statement.accept(typer));
    }

    @Test
    void aStatementDoesNotHaveAType() {
        // Expression es sealed, así que una sentencia no se puede pasar como expresión:
        // estos métodos del visitor solo se alcanzan visitando la sentencia directo
        Range range = at(6, 2);

        assertEquals(NOT_AN_EXPRESSION, visit(declare("x", "number", number(1, range))).message());
        assertEquals(NOT_AN_EXPRESSION, visit(assign("x", range, number(1, range))).message());
        assertEquals(NOT_AN_EXPRESSION, visit(println(number(1, range))).message());
        assertEquals(
                NOT_AN_EXPRESSION, visit(Ast.constant("x", "number", number(1, range))).message());
    }
}
