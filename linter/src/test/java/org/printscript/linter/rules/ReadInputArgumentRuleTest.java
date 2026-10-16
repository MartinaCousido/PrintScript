package org.printscript.linter.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Identifier;
import org.printscript.ast.Operator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.ReadInput;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.linter.Violation;
import org.printscript.linter.config.LinterConfiguration;

class ReadInputArgumentRuleTest {

    private static final Range R = new Range(new Position(1, 1), new Position(1, 1));

    private final ReadInputArgumentRule rule = new ReadInputArgumentRule();

    private static VariableDeclaration declarationWith(ReadInput read) {
        return new VariableDeclaration(
                new Identifier("input", R), new TypeName("string", R), Optional.of(read), R);
    }

    private static ReadInput readOf(org.printscript.ast.Expression prompt) {
        return new ReadInput(prompt, R);
    }

    @Test
    void acceptsALiteralPrompt() {
        Statement statement = declarationWith(readOf(new StringLiteral("Name: ", R)));

        assertEquals(List.of(), rule.check(statement));
    }

    @Test
    void acceptsAnIdentifierPrompt() {
        Statement statement = declarationWith(readOf(new Identifier("prompt", R)));

        assertEquals(List.of(), rule.check(statement));
    }

    // el caso del TCK: let input: string = readInput("Enter" + "something");
    @Test
    void rejectsAnExpressionPromptInsideADeclaration() {
        ReadInput read =
                readOf(
                        new BinaryExpression(
                                new StringLiteral("Enter", R),
                                Operator.PLUS,
                                new StringLiteral("something", R),
                                R));

        List<Violation> violations = rule.check(declarationWith(read));

        assertEquals(1, violations.size());
        assertTrue(violations.getFirst().message().contains("readInput"));
        assertEquals(LinterConfiguration.READ_INPUT_ARGUMENT_KEY, violations.getFirst().rule());
    }

    @Test
    void findsReadInputInsideOtherStatementsToo() {
        ReadInput read =
                readOf(
                        new BinaryExpression(
                                new StringLiteral("a", R),
                                Operator.PLUS,
                                new StringLiteral("b", R),
                                R));

        assertEquals(1, rule.check(new PrintlnStatement(read, R)).size());
    }

    @Test
    void reportsNothingWhenThereIsNoReadInput() {
        Statement statement = new PrintlnStatement(new StringLiteral("hola", R), R);

        assertEquals(List.of(), rule.check(statement));
    }
}
