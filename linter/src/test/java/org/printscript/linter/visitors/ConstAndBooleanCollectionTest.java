package org.printscript.linter.visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.Block;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Identifier;
import org.printscript.ast.IfStatement;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.TypeName;
import org.printscript.common.Position;
import org.printscript.common.Range;

// las construcciones de 1.1 tienen que entrar en el mismo recorrido que las de 1.0:
// un nombre mal formateado adentro de una constante se reporta igual
class ConstAndBooleanCollectionTest {

    private static final String CONDICION = "la_condicion";

    private static final Range RANGE = new Range(new Position(1, 1), new Position(1, 5));

    private final IdentifierCollector collector = new IdentifierCollector();

    private List<String> namesIn(org.printscript.ast.Statement statement) {
        return statement.accept(collector).stream().map(Identifier::name).toList();
    }

    @Test
    void aConstantContributesItsNameAndTheNamesOfItsInitializer() {
        ConstDeclaration constant =
                new ConstDeclaration(
                        new Identifier("mi_constante", RANGE),
                        new TypeName("number", RANGE),
                        Optional.of(new Identifier("otra", RANGE)),
                        RANGE);

        assertEquals(List.of("mi_constante", "otra"), namesIn(constant));
    }

    @Test
    void aConstantWithoutInitializerContributesOnlyItsName() {
        ConstDeclaration constant =
                new ConstDeclaration(
                        new Identifier("x", RANGE),
                        new TypeName("number", RANGE),
                        Optional.empty(),
                        RANGE);

        assertEquals(List.of("x"), namesIn(constant));
    }

    @Test
    void aBlockContributesTheNamesOfEveryStatementInside() {
        Block block =
                new Block(
                        List.of(
                                new PrintlnStatement(new Identifier("de_adentro", RANGE), RANGE),
                                new PrintlnStatement(new Identifier("otro_mas", RANGE), RANGE)),
                        RANGE);

        assertEquals(List.of("de_adentro", "otro_mas"), namesIn(block));
    }

    @Test
    void anIfContributesItsConditionAndBothBranches() {
        // sin esto, un nombre mal formateado adentro de un if se escaparía del linter
        Block thenBlock =
                new Block(
                        List.of(new PrintlnStatement(new Identifier("en_el_then", RANGE), RANGE)),
                        RANGE);
        Block elseBlock =
                new Block(
                        List.of(new PrintlnStatement(new Identifier("en_el_else", RANGE), RANGE)),
                        RANGE);
        IfStatement conditional =
                new IfStatement(
                        new Identifier(CONDICION, RANGE), thenBlock, Optional.of(elseBlock), RANGE);

        assertEquals(List.of(CONDICION, "en_el_then", "en_el_else"), namesIn(conditional));
    }

    @Test
    void anIfWithoutElseContributesOnlyItsConditionAndThenBranch() {
        IfStatement conditional =
                new IfStatement(
                        new Identifier(CONDICION, RANGE),
                        new Block(List.of(), RANGE),
                        Optional.empty(),
                        RANGE);

        assertEquals(List.of(CONDICION), namesIn(conditional));
    }

    @Test
    void aReadContributesTheNamesOfItsArgument() {
        assertEquals(
                List.of("el_mensaje"),
                namesIn(
                        new PrintlnStatement(
                                new ReadInput(new Identifier("el_mensaje", RANGE), RANGE), RANGE)));
        assertEquals(
                List.of("la_variable"),
                namesIn(
                        new PrintlnStatement(
                                new ReadEnv(new Identifier("la_variable", RANGE), RANGE), RANGE)));
    }

    @Test
    void aBooleanLiteralContributesNoNames() {
        assertEquals(
                List.of(), namesIn(new PrintlnStatement(new BooleanLiteral(true, RANGE), RANGE)));
    }
}
