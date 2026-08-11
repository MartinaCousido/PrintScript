package org.printscript.common;
//salió bien y trae el valor que se obtuvo
public record Success<T>(T value) implements Result<T> {
}
