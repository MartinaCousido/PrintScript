package org.printscript.ast;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;

/** Left-to-right, short-circuiting expression fold without recursive Java calls. */
public final class ExpressionFold {
    private ExpressionFold() {}

    @FunctionalInterface
    public interface BinaryReducer<T> {
        Result<T> apply(BinaryExpression expression, T left, T right);
    }

    public static <T> Result<T> evaluate(
            Expression root,
            Function<Expression, Result<T>> leaf,
            BinaryReducer<T> binary,
            BiFunction<Expression, T, Result<T>> read) {
        Deque<Frame<T>> pending = new ArrayDeque<>();
        Expression current = root;
        Result<T> result;
        while (true) {
            current = descend(current, pending);
            result = leaf.apply(current);
            while (true) {
                if (result instanceof Failure<T> || pending.isEmpty()) {
                    return result;
                }
                T value = ((Success<T>) result).value();
                Frame<T> frame = pending.peek();
                if (frame.expression instanceof BinaryExpression expression) {
                    if (!frame.hasLeft) {
                        frame.left = value;
                        frame.hasLeft = true;
                        current = expression.right();
                        break;
                    }
                    result = binary.apply(expression, frame.left, value);
                } else {
                    result = read.apply(frame.expression, value);
                }
                pending.pop();
            }
        }
    }

    private static <T> Expression descend(Expression expression, Deque<Frame<T>> pending) {
        Expression current = expression;
        while (true) {
            Expression child =
                    switch (current) {
                        case BinaryExpression binary -> binary.left();
                        case ReadInput input -> input.prompt();
                        case ReadEnv environment -> environment.name();
                        default -> null;
                    };
            if (child == null) {
                return current;
            }
            pending.push(new Frame<>(current));
            current = child;
        }
    }

    private static final class Frame<T> {
        private final Expression expression;
        private T left;
        private boolean hasLeft;

        private Frame(Expression expression) {
            this.expression = expression;
        }
    }
}
