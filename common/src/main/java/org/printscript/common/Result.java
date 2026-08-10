package org.printscript.common;

public sealed interface Result<T> permits Success, Failure {
}
