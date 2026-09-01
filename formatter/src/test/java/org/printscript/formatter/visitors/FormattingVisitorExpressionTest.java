package org.printscript.formatter.visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.StringLiteral;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.formatter.config.Separators;

class FormattingVisitorExpressionTest {

    private static final Range ANY = new Range(new Position(1, 1), new Position(1, 1));

    private final FormattingVisitor visitor =
            new FormattingVisitor(new Separators(new FormatterConfiguration(false, true, true, 0)));

    private String format(Expression expression) {
        return expression.accept(visitor);
    }

    private static Expression number(double value) {
        return new NumberLiteral(value, ANY);
    }

    private static Expression binary(Expression left, Operator operator, Expression right) {
        return new BinaryExpression(left, operator, right, ANY);
    }

    @Test
    void aWholeNumberKeepsNoDecimalPart() {
        assertEquals("12", format(number(12.0)));
    }

    @Test
    void aNumberKeepsItsDecimalPart() {
        assertEquals("3.5", format(number(3.5)));
    }

    @Test
    void aStringLiteralGetsItsQuotesBack() {
        assertEquals("\"hola\"", format(new StringLiteral("hola", ANY)));
    }

    @Test
    void anIdentifierIsItsName() {
        assertEquals("nombre", format(new Identifier("nombre", ANY)));
    }

    @Test
    void anOperatorAlwaysTakesASpaceOnBothSides() {
        assertEquals("1 + 2", format(binary(number(1), Operator.PLUS, number(2))));
    }

    @Test
    void everyOperatorHasItsOwnSymbol() {
        assertEquals("1 - 2", format(binary(number(1), Operator.MINUS, number(2))));
        assertEquals("1 * 2", format(binary(number(1), Operator.MULTIPLY, number(2))));
        assertEquals("1 / 2", format(binary(number(1), Operator.DIVIDE, number(2))));
    }

    @Test
    void aLowerPrecedenceChildOnTheLeftKeepsItsParentheses() {
        Expression sum = binary(number(1), Operator.PLUS, number(2));

        assertEquals("(1 + 2) * 3", format(binary(sum, Operator.MULTIPLY, number(3))));
    }

    @Test
    void aLowerPrecedenceChildOnTheRightKeepsItsParentheses() {
        Expression sum = binary(number(2), Operator.PLUS, number(3));

        assertEquals("1 * (2 + 3)", format(binary(number(1), Operator.MULTIPLY, sum)));
    }

    @Test
    void aHigherPrecedenceChildDoesNotGetParenthesesItDoesNotNeed() {
        Expression product = binary(number(2), Operator.MULTIPLY, number(3));

        assertEquals("1 + 2 * 3", format(binary(number(1), Operator.PLUS, product)));
    }

    @Test
    void aLeftAssociativeChainDoesNotGetParenthesesItDoesNotNeed() {
        Expression firstSubtraction = binary(number(1), Operator.MINUS, number(2));

        assertEquals("1 - 2 - 3", format(binary(firstSubtraction, Operator.MINUS, number(3))));
    }

    @Test
    void aSubtractionOnTheRightOfASubtractionKeepsItsParentheses() {
        Expression subtraction = binary(number(2), Operator.MINUS, number(3));

        assertEquals("1 - (2 - 3)", format(binary(number(1), Operator.MINUS, subtraction)));
    }

    @Test
    void aDivisionOnTheRightOfADivisionKeepsItsParentheses() {
        Expression division = binary(number(2), Operator.DIVIDE, number(3));

        assertEquals("1 / (2 / 3)", format(binary(number(1), Operator.DIVIDE, division)));
    }

    @Test
    void aProductOnTheRightOfAProductDoesNotNeedParentheses() {
        Expression product = binary(number(2), Operator.MULTIPLY, number(3));

        assertEquals("1 * 2 * 3", format(binary(number(1), Operator.MULTIPLY, product)));
    }
}
