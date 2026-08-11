package org.printscript.parser;

import java.util.List;
import java.util.NoSuchElementException;
import org.printscript.common.PrintScriptIterator;
import org.printscript.token.Token;
//convierte una lista de tokens común en el mismo PrintScriptIterator
final class ListTokenIterator implements PrintScriptIterator<Token> {

    private final List<Token> tokens;
    private int index;

    ListTokenIterator(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
        this.index = 0;
    }

    @Override
    public boolean hasNext() {
        return index < tokens.size();
    }

    @Override
    public Token getNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Token token = tokens.get(index);
        index = index + 1;
        return token;
    }
}
