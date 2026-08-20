package org.printscript.lexer.providers;

import java.util.Iterator;
import org.printscript.lexer.Lexer;

// armador de lexer: le das las lineas, te devuelve un lexer listo
@FunctionalInterface
public interface LexerProvider {

    Lexer provide(Iterator<String> lines);
}
