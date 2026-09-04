package org.printscript.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.printscript.semantic.Ast.assign;
import static org.printscript.semantic.Ast.at;
import static org.printscript.semantic.Ast.binary;
import static org.printscript.semantic.Ast.declare;
import static org.printscript.semantic.Ast.name;
import static org.printscript.semantic.Ast.number;
import static org.printscript.semantic.Ast.println;
import static org.printscript.semantic.Ast.text;

import org.junit.jupiter.api.Test;
import org.printscript.ast.Expression;
import org.printscript.ast.Operator;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;

class StatementCheckerTest {

    private static final String NUMBER = "number";
    private static final String STRING = "string";
    private static final String HOLA = "hola";
    private static final String NOT_A_STATEMENT = "No es una sentencia";

    private final StatementChecker checker = new StatementChecker();

    private Result<Void> check(Statement statement) {
        return statement.accept(checker);
    }

    private Failure<Void> failureOf(Statement statement) {
        return assertInstanceOf(Failure.class, check(statement));
    }

    private void assertPasses(Statement statement) {
        assertInstanceOf(Success.class, check(statement));
    }

    // --- declaración ---

    @Test
    void aDeclarationWhoseInitializerMatchesTheDeclaredTypePasses() {
        assertPasses(declare("x", NUMBER, number(12, Ast.ANY)));
    }

    @Test
    void aDeclarationWithoutInitializerPassesAndDeclaresTheVariable() {
        assertPasses(declare("x", NUMBER, null));

        assertPasses(println(name("x", Ast.ANY)));
    }

    @Test
    void aDeclarationWhoseInitializerHasAnotherTypeFailsAtTheInitializer() {
        Range initializer = at(1, 20);

        Failure<Void> failure = failureOf(declare("x", NUMBER, text(HOLA, initializer)));

        assertEquals("No se puede asignar string a una variable number", failure.message());
        assertEquals(initializer, failure.range());
    }

    @Test
    void aDeclarationWithATypeThatDoesNotExistFailsAtTheTypeAnnotation() {
        Range type = at(1, 8);

        Failure<Void> failure =
                failureOf(declare("x", Ast.ANY, "boolean", type, number(1, Ast.ANY)));

        assertEquals("Tipo desconocido: boolean", failure.message());
        assertEquals(type, failure.range());
    }

    @Test
    void declaringTheSameNameTwiceFailsAtTheName() {
        Range second = at(2, 5);
        assertPasses(declare("x", NUMBER, number(1, Ast.ANY)));

        Failure<Void> failure =
                failureOf(declare("x", second, NUMBER, Ast.ANY, number(2, Ast.ANY)));

        assertEquals("Variable ya declarada: x", failure.message());
        assertEquals(second, failure.range());
    }

    @Test
    void aVariableWithAWrongInitializerIsStillDeclared() {
        // el error de una sentencia no puede arrastrar a las que la usan después
        failureOf(declare("x", NUMBER, text(HOLA, Ast.ANY)));

        assertPasses(println(name("x", Ast.ANY)));
    }

    // --- asignación ---

    @Test
    void assigningTheDeclaredTypePasses() {
        assertPasses(declare("x", NUMBER, null));

        assertPasses(assign("x", Ast.ANY, number(5, Ast.ANY)));
    }

    @Test
    void assigningToAVariableThatWasNeverDeclaredFailsAtTheName() {
        Range variable = at(4, 1);

        Failure<Void> failure = failureOf(assign("x", variable, number(5, Ast.ANY)));

        assertEquals("Variable no declarada: x", failure.message());
        assertEquals(variable, failure.range());
    }

    @Test
    void assigningAnotherTypeFailsAtTheExpression() {
        Range expression = at(4, 5);
        assertPasses(declare("x", NUMBER, null));

        Failure<Void> failure = failureOf(assign("x", Ast.ANY, text(HOLA, expression)));

        assertEquals("No se puede asignar string a una variable number", failure.message());
        assertEquals(expression, failure.range());
    }

    // --- expresiones binarias ---

    @Test
    void addingTwoNumbersIsANumber() {
        Expression sum = binary(number(1, Ast.ANY), Operator.PLUS, number(2, Ast.ANY));

        assertPasses(declare("x", NUMBER, sum));
    }

    @Test
    void addingAStringToANumberIsAString() {
        Expression concatenation = binary(text("n: ", Ast.ANY), Operator.PLUS, number(2, Ast.ANY));

        assertPasses(declare("x", STRING, concatenation));
    }

    @Test
    void arithmeticOnAStringFailsAtTheOffendingLeftOperand() {
        Range left = at(1, 15);
        Expression subtraction = binary(text(HOLA, left), Operator.MINUS, number(2, Ast.ANY));

        Failure<Void> failure = failureOf(declare("x", NUMBER, subtraction));

        assertEquals(
                "El operador MINUS requiere operandos number y recibió string", failure.message());
        assertEquals(left, failure.range());
    }

    @Test
    void arithmeticOnAStringFailsAtTheOffendingRightOperand() {
        Range right = at(1, 25);
        Expression division = binary(number(2, Ast.ANY), Operator.DIVIDE, text(HOLA, right));

        Failure<Void> failure = failureOf(declare("x", NUMBER, division));

        assertEquals(right, failure.range());
    }

    @Test
    void arithmeticBetweenTwoNumbersIsANumber() {
        Expression division = binary(number(12, Ast.ANY), Operator.DIVIDE, number(4, Ast.ANY));

        assertPasses(declare("x", NUMBER, division));
    }

    @Test
    void anUndeclaredVariableOnTheLeftOfAnExpressionFailsAtItsUse() {
        Range use = at(3, 5);
        Expression sum = binary(name("y", use), Operator.PLUS, number(1, Ast.ANY));

        Failure<Void> failure = failureOf(declare("x", NUMBER, sum));

        assertEquals("Variable no declarada: y", failure.message());
        assertEquals(use, failure.range());
    }

    @Test
    void anUndeclaredVariableInsideAnExpressionFailsAtItsUse() {
        Range use = at(3, 11);
        Expression sum = binary(number(1, Ast.ANY), Operator.PLUS, name("y", use));

        Failure<Void> failure = failureOf(declare("x", NUMBER, sum));

        assertEquals("Variable no declarada: y", failure.message());
        assertEquals(use, failure.range());
    }

    // --- println ---

    @Test
    void printlnAcceptsAnyBasicType() {
        assertPasses(println(number(1, Ast.ANY)));
        assertPasses(println(text(HOLA, Ast.ANY)));
    }

    @Test
    void printlnOfAnUndeclaredVariableFailsAtItsUse() {
        Range use = at(5, 9);

        Failure<Void> failure = failureOf(println(name("x", use)));

        assertEquals("Variable no declarada: x", failure.message());
        assertEquals(use, failure.range());
    }

    // --- una expresión suelta no es una sentencia ---

    @Test
    void anExpressionIsNotAStatement() {
        // Statement es sealed, así que una expresión no se puede pasar como sentencia:
        // estos métodos del visitor solo se alcanzan visitando la expresión directo
        Range range = at(7, 3);

        assertEquals(NOT_A_STATEMENT, visit(number(1, range)).message());
        assertEquals(NOT_A_STATEMENT, visit(text(HOLA, range)).message());
        assertEquals(NOT_A_STATEMENT, visit(name("x", range)).message());
        assertEquals(
                NOT_A_STATEMENT,
                visit(binary(number(1, range), Operator.PLUS, number(2, range))).message());
        assertEquals(range, visit(number(1, range)).range());
    }

    private Failure<Void> visit(Expression expression) {
        return assertInstanceOf(Failure.class, expression.accept(checker));
    }
}
