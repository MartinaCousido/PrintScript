package org.printscript.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.printscript.semantic.Ast.assign;
import static org.printscript.semantic.Ast.declare;
import static org.printscript.semantic.Ast.name;
import static org.printscript.semantic.Ast.number;
import static org.printscript.semantic.Ast.println;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.printscript.ast.Block;
import org.printscript.ast.IfStatement;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;

// los mismos casos que IfEndToEndTest corre contra la ejecución, acá contra la
// validación: si las dos etapas no abren y cierran el frame en el mismo lugar, el modo
// Validation y la ejecución dirían cosas distintas
class ScopeTest {

    private static final String NUMBER = "number";
    private static final String BOOLEAN = "boolean";

    private final StatementChecker checker =
            new StatementChecker(Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN));

    private Result<Void> check(Statement statement) {
        return statement.accept(checker);
    }

    private static Block block(Statement... statements) {
        return new Block(List.of(statements), Ast.ANY);
    }

    private static Statement conditional(Block thenBlock, Statement elseBranch) {
        return new IfStatement(
                Ast.bool(true, Ast.ANY), thenBlock, Optional.ofNullable(elseBranch), Ast.ANY);
    }

    // --- SymbolTable ---

    @Test
    void aNameResolvesFromTheInnermostFrameOutwards() {
        SymbolTable outer =
                new SymbolTable().declare("x", new Symbol(Type.NUMBER, false)).orElseThrow();
        SymbolTable inner =
                outer.openScope().declare("x", new Symbol(Type.STRING, false)).orElseThrow();

        assertEquals(Type.STRING, inner.read("x").orElseThrow().type());
        assertEquals(Type.NUMBER, inner.closeScope().read("x").orElseThrow().type());
    }

    @Test
    void aNameDeclaredInsideAFrameDisappearsWhenItCloses() {
        SymbolTable inner =
                new SymbolTable()
                        .openScope()
                        .declare("x", new Symbol(Type.NUMBER, false))
                        .orElseThrow();

        assertTrue(inner.closeScope().read("x").isEmpty());
    }

    @Test
    void redeclaringIsOnlyRejectedWithinTheSameFrame() {
        SymbolTable outer =
                new SymbolTable().declare("x", new Symbol(Type.NUMBER, false)).orElseThrow();

        assertTrue(outer.declare("x", new Symbol(Type.NUMBER, false)).isEmpty());
        assertTrue(outer.openScope().declare("x", new Symbol(Type.NUMBER, false)).isPresent());
    }

    @Test
    void closingTheOutermostFrameIsANoOp() {
        SymbolTable table =
                new SymbolTable().declare("x", new Symbol(Type.NUMBER, false)).orElseThrow();

        assertTrue(table.closeScope().read("x").isPresent());
    }

    // --- el chequeo del if ---

    @Test
    void aVariableDeclaredInsideABlockDoesNotEscape() {
        assertInstanceOf(
                Success.class,
                check(conditional(block(declare("x", NUMBER, number(1, Ast.ANY))), null)));

        assertInstanceOf(Failure.class, check(println(name("x", Ast.ANY))));
    }

    @Test
    void theSameNameCanBeDeclaredInThenAndInElse() {
        Statement statement =
                conditional(
                        block(declare("y", NUMBER, number(1, Ast.ANY))),
                        block(declare("y", NUMBER, number(2, Ast.ANY))));

        assertInstanceOf(Success.class, check(statement));
    }

    @Test
    void anInnerDeclarationShadowsTheOuterOne() {
        assertInstanceOf(Success.class, check(declare("z", NUMBER, number(1, Ast.ANY))));

        assertInstanceOf(
                Success.class,
                check(conditional(block(declare("z", NUMBER, number(2, Ast.ANY))), null)));
    }

    @Test
    void anOuterVariableCanBeAssignedFromInsideABlock() {
        assertInstanceOf(Success.class, check(declare("n", NUMBER, number(1, Ast.ANY))));

        assertInstanceOf(
                Success.class,
                check(conditional(block(assign("n", Ast.ANY, number(2, Ast.ANY))), null)));
    }

    @Test
    void declaringTheSameNameTwiceInTheSameBlockIsStillAnError() {
        Statement statement =
                conditional(
                        block(
                                declare("x", NUMBER, number(1, Ast.ANY)),
                                declare("x", NUMBER, number(2, Ast.ANY))),
                        null);

        assertEquals(
                "Variable ya declarada: x",
                assertInstanceOf(Failure.class, check(statement)).message());
    }

    @Test
    void theFrameIsClosedEvenWhenAStatementInsideFailed() {
        check(
                conditional(
                        block(
                                declare("x", NUMBER, number(1, Ast.ANY)),
                                println(name("noExiste", Ast.ANY))),
                        null));

        // si el frame hubiera quedado abierto, la x de adentro seguiría visible
        assertInstanceOf(Failure.class, check(println(name("x", Ast.ANY))));
    }

    @Test
    void theConditionHasToBeBoolean() {
        Statement statement =
                new IfStatement(number(5, Ast.ANY), block(), Optional.empty(), Ast.ANY);

        assertEquals(
                "La condición de un if tiene que ser boolean",
                assertInstanceOf(Failure.class, check(statement)).message());
    }

    @Test
    void aBooleanVariableIsAValidCondition() {
        assertInstanceOf(Success.class, check(declare("flag", BOOLEAN, Ast.bool(true, Ast.ANY))));

        Statement statement =
                new IfStatement(name("flag", Ast.ANY), block(), Optional.empty(), Ast.ANY);

        assertInstanceOf(Success.class, check(statement));
    }

    @Test
    void theConditionIsCheckedInTheOuterScope() {
        // la condición de un else if no ve lo que se declaró en el then anterior
        assertInstanceOf(
                Success.class,
                check(
                        conditional(
                                block(declare("interna", BOOLEAN, Ast.bool(true, Ast.ANY))),
                                null)));

        Statement statement =
                new IfStatement(name("interna", Ast.ANY), block(), Optional.empty(), Ast.ANY);

        assertInstanceOf(Failure.class, check(statement));
    }
}
