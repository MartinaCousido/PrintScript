package org.printscript.linter.visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Block;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.IfStatement;
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

class PrintlnCollectorTest {

    private static final Range RANGE = new Range(new Position(1, 1), new Position(1, 4));

    private final PrintlnCollector collector = new PrintlnCollector();

    private static PrintlnStatement println(String marker) {
        return new PrintlnStatement(new StringLiteral(marker, RANGE), RANGE);
    }

    private List<String> markersIn(Statement statement) {
        return statement.accept(collector).stream()
                .map(println -> ((StringLiteral) println.expression()).value())
                .toList();
    }

    @Test
    void aPrintlnContributesItself() {
        assertEquals(List.of("uno"), markersIn(println("uno")));
    }

    @Test
    void aPrintlnInsideABlockIsFound() {
        assertEquals(List.of("adentro"), markersIn(new Block(List.of(println("adentro")), RANGE)));
    }

    @Test
    void thePrintlnsOfBothBranchesAreFound() {
        IfStatement conditional =
                new IfStatement(
                        new BooleanLiteral(true, RANGE),
                        new Block(List.of(println("then")), RANGE),
                        Optional.of(new Block(List.of(println("else")), RANGE)),
                        RANGE);

        assertEquals(List.of("then", "else"), markersIn(conditional));
    }

    @Test
    void nestingIsFollowedAllTheWayDown() {
        IfStatement inner =
                new IfStatement(
                        new BooleanLiteral(true, RANGE),
                        new Block(List.of(println("hondo")), RANGE),
                        Optional.empty(),
                        RANGE);
        IfStatement outer =
                new IfStatement(
                        new BooleanLiteral(true, RANGE),
                        new Block(List.of(inner), RANGE),
                        Optional.empty(),
                        RANGE);

        assertEquals(List.of("hondo"), markersIn(outer));
    }

    @Test
    void theStatementsThatAreNotPrintlnContributeNothing() {
        Identifier name = new Identifier("x", RANGE);
        TypeName type = new TypeName("number", RANGE);
        Expression number = new NumberLiteral(1, RANGE);

        assertEquals(
                List.of(),
                markersIn(new VariableDeclaration(name, type, Optional.of(number), RANGE)));
        assertEquals(
                List.of(), markersIn(new ConstDeclaration(name, type, Optional.of(number), RANGE)));
        assertEquals(List.of(), markersIn(new AssignmentStatement(name, number, RANGE)));
    }

    @Test
    void theExpressionsContributeNothingOnTheirOwn() {
        assertEquals(List.of(), new NumberLiteral(1, RANGE).accept(collector));
        assertEquals(List.of(), new StringLiteral("x", RANGE).accept(collector));
        assertEquals(List.of(), new BooleanLiteral(true, RANGE).accept(collector));
        assertEquals(List.of(), new Identifier("x", RANGE).accept(collector));
        assertEquals(
                List.of(),
                new BinaryExpression(
                                new NumberLiteral(1, RANGE),
                                Operator.PLUS,
                                new NumberLiteral(2, RANGE),
                                RANGE)
                        .accept(collector));
        assertEquals(
                List.of(), new ReadInput(new StringLiteral("p", RANGE), RANGE).accept(collector));
        assertEquals(
                List.of(), new ReadEnv(new StringLiteral("P", RANGE), RANGE).accept(collector));
    }
}
