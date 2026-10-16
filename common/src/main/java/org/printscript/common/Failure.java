package org.printscript.common;

import java.util.Objects;

// falló y trae un mensaje y el Range donde pasó
public record Failure<T>(String message, Range range) implements Result<T> {

    public Failure {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(range, "range");
    }
}
