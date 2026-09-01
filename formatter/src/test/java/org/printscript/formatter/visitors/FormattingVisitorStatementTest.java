package org.printscript.formatter.visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.formatter.config.Separators;

class FormattingVisitorStatementTest {

    private static final Range ANY = new Range(new Position(1, 1), new Position(1, 1));

    private static String format(Statement statement, FormatterConfiguration configuration) {
        return statement.accept(new FormattingVisitor(new Separators(configuration)));
    }

    private static Statement declaration(Expression initializer) {
        return new VariableDeclaration(
                new Identifier("x", ANY),
                new TypeName("number", ANY),
                Optional.ofNullable(initializer),
                ANY);
    }

    @Test
    void aDeclarationWithTightColonAndTightAssign() {
        FormatterConfiguration tight = new FormatterConfiguration(false, false, false, 0);

        assertEquals("let x:number=12;", format(declaration(new NumberLiteral(12, ANY)), tight));
    }

    @Test
    void aDeclarationWithASpaceAfterTheColonAndAroundTheAssign() {
        FormatterConfiguration spaced = new FormatterConfiguration(false, true, true, 0);

        assertEquals(
                "let x: number = 12;", format(declaration(new NumberLiteral(12, ANY)), spaced));
    }

    @Test
    void aDeclarationWithASpaceOnBothSidesOfTheColon() {
        FormatterConfiguration spaced = new FormatterConfiguration(true, true, true, 0);

        assertEquals(
                "let x : number = 12;", format(declaration(new NumberLiteral(12, ANY)), spaced));
    }

    @Test
    void aDeclarationWithoutAnInitializerHasNoAssign() {
        FormatterConfiguration spaced = new FormatterConfiguration(false, true, true, 0);

        assertEquals("let x: number;", format(declaration(null), spaced));
    }

    @Test
    void anAssignmentUsesTheAssignSeparator() {
        Statement assignment =
                new AssignmentStatement(new Identifier("x", ANY), new NumberLiteral(3, ANY), ANY);

        assertEquals(
                "x = 3;", format(assignment, new FormatterConfiguration(false, true, true, 0)));
        assertEquals("x=3;", format(assignment, new FormatterConfiguration(false, true, false, 0)));
    }

    @Test
    void aPrintlnWrapsItsExpressionInParenthesesWithoutInnerSpaces() {
        Statement println = new PrintlnStatement(new StringLiteral("hola", ANY), ANY);

        assertEquals(
                "println(\"hola\");",
                format(println, new FormatterConfiguration(false, true, true, 0)));
    }

    @Test
    void aPrintlnKeepsTheSpacingOfTheExpressionInside() {
        Statement println =
                new PrintlnStatement(
                        new BinaryExpression(
                                new StringLiteral("Result: ", ANY),
                                Operator.PLUS,
                                new Identifier("a", ANY),
                                ANY),
                        ANY);

        assertEquals(
                "println(\"Result: \" + a);",
                format(println, new FormatterConfiguration(false, true, true, 0)));
    }
}
