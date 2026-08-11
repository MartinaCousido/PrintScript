package org.printscript.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;

class ExpressionTest {

    private static Range rangeAt(int startColumn, int endColumn) {
        return new Range(new Position(1, startColumn), new Position(1, endColumn));
    }

    private static String describe(Expression expression) {
        return switch (expression) {
            case NumberLiteral number -> "number:" + number.value();
            case StringLiteral string -> "string:" + string.value();
            case Identifier identifier -> "identifier:" + identifier.name();
            case BinaryExpression binary -> "binary:" + binary.operator();
        };
    }

    @Test
    void switchOverExpressionIsExhaustiveWithoutDefault() {
        assertEquals("number:4.0", describe(new NumberLiteral(4.0, rangeAt(1, 1))));
        assertEquals("string:hi", describe(new StringLiteral("hi", rangeAt(1, 4))));
        assertEquals("identifier:x", describe(new Identifier("x", rangeAt(1, 1))));

        BinaryExpression sum = new BinaryExpression(
            new NumberLiteral(1.0, rangeAt(1, 1)),
            Operator.PLUS,
            new NumberLiteral(2.0, rangeAt(5, 5)),
            rangeAt(1, 5));

        assertEquals("binary:PLUS", describe(sum));
    }

    @Test
    void everyExpressionExposesItsRange() {
        Range numberRange = rangeAt(1, 2);
        Range stringRange = rangeAt(4, 9);
        Range identifierRange = rangeAt(11, 11);
        Range binaryRange = rangeAt(1, 11);

        NumberLiteral number = new NumberLiteral(42.0, numberRange);
        StringLiteral string = new StringLiteral("hello", stringRange);
        Identifier identifier = new Identifier("y", identifierRange);
        BinaryExpression binary = new BinaryExpression(number, Operator.MINUS, identifier, binaryRange);

        assertEquals(numberRange, number.range());
        assertEquals(stringRange, string.range());
        assertEquals(identifierRange, identifier.range());
        assertEquals(binaryRange, binary.range());
    }

    @Test
    void operatorEnumHasTheFourArithmeticOperations() {
        assertEquals(4, Operator.values().length);
        assertEquals(Operator.PLUS, Operator.valueOf("PLUS"));
        assertEquals(Operator.MINUS, Operator.valueOf("MINUS"));
        assertEquals(Operator.MULTIPLY, Operator.valueOf("MULTIPLY"));
        assertEquals(Operator.DIVIDE, Operator.valueOf("DIVIDE"));
    }
}
