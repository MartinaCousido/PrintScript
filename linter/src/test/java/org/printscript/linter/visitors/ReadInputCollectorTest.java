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
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Position;
import org.printscript.common.Range;

// el colector tiene que encontrar el readInput esté donde esté: es lo que hace que la
// regla valga adentro de un if o de un bloque, y no solo en el nivel de arriba
class ReadInputCollectorTest {

    private static final Range R = new Range(new Position(1, 1), new Position(1, 1));

    private final ReadInputCollector collector = new ReadInputCollector();

    private static ReadInput read() {
        return new ReadInput(new StringLiteral("prompt", R), R);
    }

    private static VariableDeclaration declaration(Expression initializer) {
        return new VariableDeclaration(
                new Identifier("x", R), new TypeName("string", R), Optional.of(initializer), R);
    }

    private int found(Statement statement) {
        return statement.accept(collector).size();
    }

    @Test
    void findsItInTheInitializerOfALetAndOfAConst() {
        assertEquals(1, found(declaration(read())));
        assertEquals(
                1,
                found(
                        new ConstDeclaration(
                                new Identifier("x", R),
                                new TypeName("string", R),
                                Optional.of(read()),
                                R)));
    }

    @Test
    void findsNothingInADeclarationWithoutInitializer() {
        Statement sinInicializador =
                new VariableDeclaration(
                        new Identifier("x", R), new TypeName("string", R), Optional.empty(), R);

        assertEquals(0, found(sinInicializador));
    }

    @Test
    void findsItInsideAnAssignmentAndAPrintln() {
        assertEquals(1, found(new AssignmentStatement(new Identifier("x", R), read(), R)));
        assertEquals(1, found(new PrintlnStatement(read(), R)));
    }

    @Test
    void findsItOnBothSidesOfABinaryExpression() {
        Expression both =
                new BinaryExpression(read(), org.printscript.ast.Operator.PLUS, read(), R);

        assertEquals(2, found(declaration(both)));
    }

    @Test
    void descendsIntoBlocks() {
        Block block = new Block(List.of(new PrintlnStatement(read(), R), declaration(read())), R);

        assertEquals(2, found(block));
    }

    @Test
    void descendsIntoTheConditionTheThenAndTheElseOfAnIf() {
        IfStatement ifStatement =
                new IfStatement(
                        read(),
                        new Block(List.of(new PrintlnStatement(read(), R)), R),
                        Optional.of(new Block(List.of(declaration(read())), R)),
                        R);

        assertEquals(3, found(ifStatement));
    }

    @Test
    void descendsIntoTheArgumentOfAReadEnv() {
        assertEquals(1, found(declaration(new ReadEnv(read(), R))));
    }

    @Test
    void findsNothingInLiteralsOrIdentifiers() {
        assertEquals(0, found(declaration(new StringLiteral("a", R))));
        assertEquals(0, found(declaration(new NumberLiteral(1, R))));
        assertEquals(0, found(declaration(new BooleanLiteral(true, R))));
        assertEquals(0, found(declaration(new Identifier("otra", R))));
    }

    @Test
    void descendsIntoThePromptOfAnotherReadInput() {
        ReadInput inner = read();
        ReadInput outer = new ReadInput(inner, R);
        assertEquals(List.of(outer, inner), outer.accept(collector));
    }

    @Test
    void deepPromptsAndBlocksAreCollectedWithoutJvmRecursion() {
        final int depth = 10_000;
        Expression prompt = new StringLiteral("prompt", R);
        for (int index = 0; index < depth; index++) {
            prompt = new ReadInput(prompt, R);
        }
        Statement statement = new PrintlnStatement(prompt, R);
        for (int index = 0; index < depth; index++) {
            statement = new Block(List.of(statement), R);
        }
        assertEquals(depth, found(statement));
        assertEquals(1, statement.accept(new PrintlnCollector()).size());
        assertEquals(0, statement.accept(new IdentifierCollector()).size());
    }
}
