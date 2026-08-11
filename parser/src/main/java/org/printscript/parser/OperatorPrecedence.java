package org.printscript.parser;

import java.util.Map;
import java.util.Optional;
import org.printscript.ast.Operator;
import org.printscript.token.Token;
import org.printscript.token.TokenType;
//tabla de prioridades; el número más alto se resuelve
final class OperatorPrecedence {

    private static final Map<String, Operator> OPERATORS = Map.of(
        "+", Operator.PLUS,
        "-", Operator.MINUS,
        "*", Operator.MULTIPLY,
        "/", Operator.DIVIDE);

    private static final Map<Operator, Integer> PRECEDENCES = Map.of(
        Operator.PLUS, 1,
        Operator.MINUS, 1,
        Operator.MULTIPLY, 2,
        Operator.DIVIDE, 2);

    private OperatorPrecedence() {
    }

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
