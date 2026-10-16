package org.printscript.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.printscript.token.Token;

// una ventanita sobre los tokens que van llegando
// guarda los tokens que hacen falta ahora; hace posible no tener tod en memoria
public final class TokenBuffer {

    private final Iterator<Token> source;
    private final List<Token> retained;

    public TokenBuffer(Iterator<Token> source) {
        this.source = Objects.requireNonNull(source, "source");
        this.retained = new ArrayList<>();
    }

    // permite espiar sin consumir
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
            retained.add(source.next());
        }
    }
}
