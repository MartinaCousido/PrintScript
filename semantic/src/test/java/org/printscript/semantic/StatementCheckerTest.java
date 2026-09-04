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

import java.util.Set;
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

    private final StatementChecker checker =
            new StatementChecker(Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN));

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

        Failure<Void> failure = failureOf(declare("x", Ast.ANY, "float", type, number(1, Ast.ANY)));

        assertEquals("Tipo desconocido: float", failure.message());
        assertEquals(type, failure.range());
    }

    @Test
    void declaringTheSameNameTwiceFailsAtTheWholeDeclaration() {
        assertPasses(declare("x", NUMBER, number(1, Ast.ANY)));

        Failure<Void> failure = failureOf(declare("x", NUMBER, number(2, Ast.ANY)));

        assertEquals("Variable ya declarada: x", failure.message());
        assertEquals(Ast.ANY, failure.range());
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
    void assigningToAVariableThatWasNeverDeclaredFailsAtTheWholeStatement() {
        // en una asignación la sentencia entera es la que no tiene sentido; cuando el
        // nombre aparece dentro de una expresión, el rango es el del uso
        Failure<Void> failure = failureOf(assign("x", at(4, 1), number(5, Ast.ANY)));

        assertEquals("Variable no declarada: x", failure.message());
        assertEquals(Ast.ANY, failure.range());
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
    void arithmeticOnAStringNamesBothOperandTypes() {
        // mismo mensaje y mismo rango que reportaba el intérprete para este error
        Expression subtraction = binary(text(HOLA, Ast.ANY), Operator.MINUS, text(HOLA, Ast.ANY));

        Failure<Void> failure = failureOf(declare("x", NUMBER, subtraction));

        assertEquals("No se puede aplicar MINUS entre string y string", failure.message());
        assertEquals(Ast.ANY, failure.range());
    }

    @Test
    void arithmeticFailsWhicheverSideIsNotANumber() {
        Expression division = binary(number(2, Ast.ANY), Operator.DIVIDE, text(HOLA, Ast.ANY));
        Expression product = binary(text(HOLA, Ast.ANY), Operator.MULTIPLY, number(2, Ast.ANY));

        assertEquals(
                "No se puede aplicar DIVIDE entre number y string",
                failureOf(declare("x", NUMBER, division)).message());
        assertEquals(
                "No se puede aplicar MULTIPLY entre string y number",
                failureOf(declare("y", NUMBER, product)).message());
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

    // --- constantes ---

    @Test
    void aConstantWithAMatchingInitializerPasses() {
        assertPasses(Ast.constant("x", NUMBER, number(1, Ast.ANY)));
    }

    @Test
    void aConstantWithoutAValueIsRejected() {
        // una constante sin valor no se puede completar después: nace inservible
        Failure<Void> failure = failureOf(Ast.constant("x", NUMBER, null));

        assertEquals("Una constante tiene que declararse con un valor", failure.message());
        assertEquals(Ast.ANY, failure.range());
    }

    @Test
    void aConstantCannotBeReassigned() {
        assertPasses(Ast.constant("x", NUMBER, number(1, Ast.ANY)));

        Failure<Void> failure = failureOf(assign("x", Ast.ANY, number(2, Ast.ANY)));

        assertEquals("No se puede reasignar la constante x", failure.message());
    }

    @Test
    void aConstantStillHasToMatchItsDeclaredType() {
        Failure<Void> failure = failureOf(Ast.constant("x", NUMBER, text(HOLA, Ast.ANY)));

        assertEquals("No se puede asignar string a una variable number", failure.message());
    }

    @Test
    void aConstantSharesTheNamespaceWithTheVariables() {
        assertPasses(declare("x", NUMBER, number(1, Ast.ANY)));

        assertEquals(
                "Variable ya declarada: x",
                failureOf(Ast.constant("x", NUMBER, number(2, Ast.ANY))).message());
    }

    // --- booleanos ---

    @Test
    void aBooleanInitializerMatchesABooleanDeclaration() {
        assertPasses(declare("ok", "boolean", Ast.bool(true, Ast.ANY)));
    }

    @Test
    void aBooleanDoesNotTakePartInBinaryOperations() {
        assertEquals(
                "No se puede aplicar PLUS entre boolean y number",
                failureOf(
                                declare(
                                        "x",
                                        NUMBER,
                                        binary(
                                                Ast.bool(true, Ast.ANY),
                                                Operator.PLUS,
                                                number(1, Ast.ANY))))
                        .message());
        assertEquals(
                "No se puede aplicar PLUS entre string y boolean",
                failureOf(
                                declare(
                                        "y",
                                        STRING,
                                        binary(
                                                text(HOLA, Ast.ANY),
                                                Operator.PLUS,
                                                Ast.bool(false, Ast.ANY))))
                        .message());
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
        assertEquals(NOT_A_STATEMENT, visit(Ast.bool(true, range)).message());
        assertEquals(
                NOT_A_STATEMENT,
                visit(binary(number(1, range), Operator.PLUS, number(2, range))).message());
        assertEquals(range, visit(number(1, range)).range());
    }

    private Failure<Void> visit(Expression expression) {
        return assertInstanceOf(Failure.class, expression.accept(checker));
    }
}
