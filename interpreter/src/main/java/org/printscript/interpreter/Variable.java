package org.printscript.interpreter;

// guarda el tipo que se escribió, el valor de la variable y si se puede reasignar
public record Variable(String declaredType, Object value, boolean immutable) {}
