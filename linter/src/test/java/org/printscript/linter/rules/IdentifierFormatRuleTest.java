package org.printscript.linter.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.linter.Violation;
import org.printscript.linter.config.LinterConfiguration;
import org.printscript.linter.rules.format.CamelCaseChecker;

class IdentifierFormatRuleTest {

    private static final Range ANY = new Range(new Position(1, 1), new Position(1, 1));
    private static final String NUMBER = "number";
    private static final String MAL = "user_name";

    private final IdentifierFormatRule rule = new IdentifierFormatRule(new CamelCaseChecker());

    private static Statement declarationOf(Identifier name) {
        return new VariableDeclaration(name, new TypeName(NUMBER, ANY), Optional.empty(), ANY);
    }

    @Test
    void aDeclarationWithACamelCaseNameHasNoViolations() {
        Statement declaration = declarationOf(new Identifier("userName", ANY));

        assertTrue(rule.check(declaration).isEmpty());
    }

    @Test
    void aDeclarationWithASnakeCaseNameIsAViolation() {
        Statement declaration = declarationOf(new Identifier(MAL, ANY));

        assertEquals(1, rule.check(declaration).size());
    }

    @Test
    void theViolationPointsAtTheIdentifierAndNotAtTheWholeStatement() {
        Range nameRange = new Range(new Position(4, 5), new Position(4, 13));
        Range statementRange = new Range(new Position(4, 1), new Position(4, 25));
        Statement declaration =
                new VariableDeclaration(
                        new Identifier(MAL, nameRange),
                        new TypeName(NUMBER, ANY),
                        Optional.empty(),
                        statementRange);

        assertEquals(nameRange, rule.check(declaration).get(0).range());
    }

    @Test
    void theMessageNamesTheIdentifierAndTheExpectedFormat() {
        Statement declaration = declarationOf(new Identifier(MAL, ANY));

        Violation violation = rule.check(declaration).get(0);
        String message = violation.message();

        assertEquals(LinterConfiguration.IDENTIFIER_FORMAT_KEY, violation.rule());

        assertTrue(message.contains(MAL), message);
        assertTrue(message.contains("camel"), message);
    }

    @Test
    void everyOffendingIdentifierOfTheStatementIsReported() {
        Statement assignment =
                new AssignmentStatement(
                        new Identifier(MAL, ANY),
                        new BinaryExpression(
                                new Identifier("otro_mal", ANY),
                                Operator.PLUS,
                                new NumberLiteral(1.0, ANY),
                                ANY),
                        ANY);

        List<Violation> violations = rule.check(assignment);

        assertEquals(2, violations.size());
    }

    @Test
    void theCorrectIdentifiersOfAMixedStatementAreNotReported() {
        Statement assignment =
                new AssignmentStatement(
                        new Identifier("bienEscrito", ANY), new Identifier(MAL, ANY), ANY);

        List<Violation> violations = rule.check(assignment);

        assertEquals(1, violations.size());
        assertTrue(violations.get(0).message().contains(MAL), violations.get(0).message());
    }

    @Test
    void identifiersUsedInsideAPrintlnAreAlsoChecked() {
        Statement println = new PrintlnStatement(new Identifier(MAL, ANY), ANY);

        assertEquals(1, rule.check(println).size());
    }
}
