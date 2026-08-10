package org.printscript.common;

public record Success<T>(T value) implements Result<T> {
}
