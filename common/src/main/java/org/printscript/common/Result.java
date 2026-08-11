package org.printscript.common;

//la respuesta de cualquier operación: salió bien o falló
public sealed interface Result<T> permits Success, Failure {
}
