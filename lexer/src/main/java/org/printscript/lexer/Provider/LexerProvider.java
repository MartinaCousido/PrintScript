package org.printscript.lexer.Provider;

import org.printscript.lexer.Lexer;

import java.util.Iterator;
//armador de lexer: le das las lineas, te devuelve un lexer listo
public interface LexerProvider {

    Lexer provide(Iterator<String> lines);
}
