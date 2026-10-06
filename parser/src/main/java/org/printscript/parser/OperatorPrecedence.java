package org.printscript.parser;

import java.util.Map;
import java.util.Optional;
import org.printscript.ast.Operator;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// tabla de prioridades; el número más alto se resuelve
final class OperatorPrecedence {

    private static final int ADDITIVE_PRECEDENCE = 1;
    private static final int MULTIPLICATIVE_PRECEDENCE = 2;

    private static final Map<String, Operator> OPERATORS =
            Map.of(
                    "+", Operator.PLUS,
                    "-", Operator.MINUS,
                    "*", Operator.MULTIPLY,
                    "/", Operator.DIVIDE);

    private static final Map<Operator, Integer> PRECEDENCES =
            Map.of(
                    Operator.PLUS, ADDITIVE_PRECEDENCE,
                    Operator.MINUS, ADDITIVE_PRECEDENCE,
                    Operator.MULTIPLY, MULTIPLICATIVE_PRECEDENCE,
                    Operator.DIVIDE, MULTIPLICATIVE_PRECEDENCE);

    private OperatorPrecedence() {}

    static Optional<Operator> operatorFor(Token token) {
        if (token.type() != TokenType.OPERATOR) {
            return Optional.empty();
        }
        return Optional.ofNullable(OPERATORS.get(token.value()));
    }

    static int precedenceOf(Operator operator) {
        return PRECEDENCES.get(operator);
    }
}
