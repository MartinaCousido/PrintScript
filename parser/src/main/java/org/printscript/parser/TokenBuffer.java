package org.printscript.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.printscript.common.PrintScriptIterator;
import org.printscript.token.Token;

public final class TokenBuffer {

    private final PrintScriptIterator<Token> source;
    private final List<Token> retained;

    public TokenBuffer(PrintScriptIterator<Token> source) {
        this.source = source;
        this.retained = new ArrayList<>();
    }

    public Optional<Token> lookahead(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("position must be non-negative");
        }
        fill(position + 1);
        if (position < retained.size()) {
            return Optional.of(retained.get(position));
        }
        return Optional.empty();
    }

    public void commit(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
        fill(count);
        if (retained.size() < count) {
            throw new NoSuchElementException();
        }
        retained.subList(0, count).clear();
    }

    private void fill(int size) {
        while (retained.size() < size && source.hasNext()) {
            retained.add(source.getNext());
        }
    }
}
