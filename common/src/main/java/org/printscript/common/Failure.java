package org.printscript.common;

public record Failure<T>(String message, Range range) implements Result<T> {
}
