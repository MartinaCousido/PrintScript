package org.printscript.common;
//falló y trae un mensaje y el Range donde pasó
public record Failure<T>(String message, Range range) implements Result<T> {
}
