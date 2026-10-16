package org.printscript.parser;

import java.util.Optional;
import java.util.function.Predicate;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// preguntas cortas que se repiten mucho, "este token es la palabra let?" "es un ;?"
final class Tokens {

    private Tokens() {}

    static boolean isKeyword(Token token, String text) {
        return token.type() == TokenType.KEYWORD && text.equals(token.value());
    }

    static boolean isPunctuation(Token token, String text) {
        return token.type() == TokenType.PUNCTUATION && text.equals(token.value());
    }

    static boolean isOperator(Token token, String text) {
        return token.type() == TokenType.OPERATOR && text.equals(token.value());
    }

    static Result<Token> expect(
            TokenBuffer tokens, Predicate<Token> shape, String message, Range fallback) {
        Optional<Token> next = tokens.lookahead(0);
        if (next.isEmpty()) {
            return new Failure<>(message, fallback);
        }
        if (!shape.test(next.get())) {
            return new Failure<>(message, next.get().range());
        }
        tokens.commit(1);
        return new Success<>(next.get());
    }

    static <T> Failure<T> forward(Failure<?> failure) {
        return new Failure<>(failure.message(), failure.range());
    }
}
