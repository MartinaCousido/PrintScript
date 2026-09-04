package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.Block;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.IfStatement;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.runtime.OutputPrinter;

class ScopeTest {

    private static final Range RANGE = new Range(new Position(1, 1), new Position(1, 4));
    private static final String NUMBER = "number";

    private final List<String> printed = new ArrayList<>();
    private final OutputPrinter printer =
            new OutputPrinter() {

                @Override
                public void println(String line) {
                    printed.add(line);
                }

                @Override
                public void print(String text) {
                    printed.add(text);
                }
            };
    private final Executor executor = new Executor(new Environment(), printer);

    private static Statement declare(String name, Object value) {
        return new VariableDeclaration(
                new Identifier(name, RANGE),
                new TypeName(NUMBER, RANGE),
                Optional.of(new NumberLiteral(((Number) value).doubleValue(), RANGE)),
                RANGE);
    }

    private static Statement println(Expression expression) {
        return new PrintlnStatement(expression, RANGE);
    }

    private static Block block(Statement... statements) {
        return new Block(List.of(statements), RANGE);
    }

    private static Statement conditional(boolean condition, Block thenBlock, Statement elseBranch) {
        return new IfStatement(
                new BooleanLiteral(condition, RANGE),
                thenBlock,
                Optional.ofNullable(elseBranch),
                RANGE);
    }

    private Result<Void> run(Statement statement) {
        return statement.accept(executor);
    }

    // --- Environment ---

    @Test
    void anEnvironmentResolvesNamesFromTheInnermostFrameOutwards() {
        Environment outer = new Environment().declare("x", NUMBER, 1, false).orElseThrow();
        Environment inner = outer.openScope().declare("x", NUMBER, 2, false).orElseThrow();

        assertEquals(2, inner.read("x").orElseThrow().value());
        assertEquals(1, inner.closeScope().read("x").orElseThrow().value());
    }

    @Test
    void aNameDeclaredInsideAFrameDisappearsWhenItCloses() {
        Environment inner =
                new Environment().openScope().declare("x", NUMBER, 1, false).orElseThrow();

        assertTrue(inner.closeScope().read("x").isEmpty());
    }

    @Test
    void assigningFromAnInnerFrameWritesWhereTheVariableLives() {
        Environment outer = new Environment().declare("x", NUMBER, 1, false).orElseThrow();

        Environment assigned = outer.openScope().assign("x", 2).orElseThrow();

        assertEquals(2, assigned.closeScope().read("x").orElseThrow().value());
    }

    @Test
    void theSameNameCanBeDeclaredAgainInAnInnerFrame() {
        Environment outer = new Environment().declare("x", NUMBER, 1, false).orElseThrow();

        assertTrue(outer.openScope().declare("x", NUMBER, 2, false).isPresent());
        assertTrue(outer.declare("x", NUMBER, 2, false).isEmpty());
    }

    @Test
    void closingTheOutermostFrameIsANoOp() {
        Environment environment = new Environment().declare("x", NUMBER, 1, false).orElseThrow();

        assertEquals(1, environment.closeScope().read("x").orElseThrow().value());
    }

    @Test
    void aConstantCannotBeAssigned() {
        Environment environment = new Environment().declare("c", NUMBER, 1, true).orElseThrow();

        assertTrue(environment.assign("c", 2).isEmpty());
    }

    // --- Executor ---

    @Test
    void aBlockRunsItsStatementsInOrder() {
        assertInstanceOf(
                Success.class,
                run(
                        block(
                                println(new StringLiteral("uno", RANGE)),
                                println(new StringLiteral("dos", RANGE)))));

        assertEquals(List.of("uno", "dos"), printed);
    }

    @Test
    void aBlockStopsAtTheFirstFailureAndStillClosesItsFrame() {
        Result<Void> result =
                run(
                        block(
                                declare("x", 1),
                                println(new Identifier("noExiste", RANGE)),
                                println(new StringLiteral("no llega", RANGE))));

        assertInstanceOf(Failure.class, result);
        assertEquals(List.of(), printed);
        // el frame se cerró: la x de adentro no sobrevive
        assertInstanceOf(
                Failure.class,
                run(
                        new AssignmentStatement(
                                new Identifier("x", RANGE), new NumberLiteral(2, RANGE), RANGE)));
    }

    @Test
    void anIfRunsExactlyOneBranch() {
        run(
                conditional(
                        false,
                        block(println(new StringLiteral("then", RANGE))),
                        block(println(new StringLiteral("else", RANGE)))));

        assertEquals(List.of("else"), printed);
    }

    @Test
    void anIfWithoutElseRunsNothingWhenTheConditionIsFalse() {
        assertInstanceOf(
                Success.class,
                run(conditional(false, block(println(new StringLiteral("no", RANGE))), null)));

        assertEquals(List.of(), printed);
    }

    @Test
    void aConditionThatIsNotBooleanFailsInsteadOfChoosingABranch() {
        // semantic lo rechaza antes; el intérprete no puede irse por el else en silencio
        Statement conditional =
                new IfStatement(
                        new NumberLiteral(5, RANGE),
                        block(println(new StringLiteral("no", RANGE))),
                        Optional.empty(),
                        RANGE);

        Failure<?> failure = assertInstanceOf(Failure.class, run(conditional));

        assertEquals("La condición de un if tiene que ser boolean", failure.message());
        assertEquals(List.of(), printed);
    }

    @Test
    void aFailingConditionTravelsAsData() {
        Statement conditional =
                new IfStatement(
                        new Identifier("noExiste", RANGE), block(), Optional.empty(), RANGE);

        assertInstanceOf(Failure.class, run(conditional));
    }

    @Test
    void aConstantDeclaredInsideABlockCannotBeReassignedThere() {
        Statement constant =
                new ConstDeclaration(
                        new Identifier("c", RANGE),
                        new TypeName(NUMBER, RANGE),
                        Optional.of(new NumberLiteral(1, RANGE)),
                        RANGE);

        Result<Void> result =
                run(
                        block(
                                constant,
                                new AssignmentStatement(
                                        new Identifier("c", RANGE),
                                        new NumberLiteral(2, RANGE),
                                        RANGE)));

        assertEquals(
                "No se puede reasignar la constante c",
                assertInstanceOf(Failure.class, result).message());
    }
}
