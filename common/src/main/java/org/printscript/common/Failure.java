package org.printscript.common;

import java.util.Objects;

// falló y trae qué regla se incumplió, un mensaje y el Range donde pasó
public record Failure<T>(ErrorCode code, String message, Range range) implements Result<T> {

    public Failure {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(range, "range");
    }
}
