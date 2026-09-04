package org.printscript.linter.visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Identifier;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.TypeName;
import org.printscript.common.Position;
import org.printscript.common.Range;

// las construcciones de 1.1 tienen que entrar en el mismo recorrido que las de 1.0:
// un nombre mal formateado adentro de una constante se reporta igual
class ConstAndBooleanCollectionTest {

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
    void aBooleanLiteralContributesNoNames() {
        assertEquals(
                List.of(), namesIn(new PrintlnStatement(new BooleanLiteral(true, RANGE), RANGE)));
    }
}
