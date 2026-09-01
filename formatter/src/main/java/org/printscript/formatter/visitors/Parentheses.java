package org.printscript.formatter.visitors;

import java.util.Map;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Expression;
import org.printscript.ast.Operator;

final class Parentheses {

    enum Side {
        LEFT,
        RIGHT
    }

    private static final Map<Operator, Integer> BINDING_POWER =
            Map.of(
                    Operator.PLUS, 1,
                    Operator.MINUS, 1,
                    Operator.MULTIPLY, 2,
                    Operator.DIVIDE, 2);

    private static final Map<Operator, Boolean> ASSOCIATIVE =
            Map.of(
                    Operator.PLUS, true,
                    Operator.MINUS, false,
                    Operator.MULTIPLY, true,
                    Operator.DIVIDE, false);

    private Parentheses() {}

    static boolean areNeededAround(Expression child, Operator parent, Side side) {
        if (!(child instanceof BinaryExpression binary)) {
            return false;
        }
        int childPower = BINDING_POWER.get(binary.operator());
        int parentPower = BINDING_POWER.get(parent);
        if (childPower < parentPower) {
            return true;
        }
        return childPower == parentPower && side == Side.RIGHT && !ASSOCIATIVE.get(parent);
    }
}
