package org.printscript.semantic;

// lo que la tabla de tipos sabe de un nombre: qué tipo tiene y si se puede reasignar
public record Symbol(Type type, boolean immutable) {}
