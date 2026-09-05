package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class TokenBufferTest {

    private static final class ListTokenSource implements Iterator<Token> {

        private final List<Token> tokens;
        private int index;

        private ListTokenSource(List<Token> tokens) {
            this.tokens = tokens;
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < tokens.size();
        }

        @Override
        public Token next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Token token = tokens.get(index);
            index = index + 1;
            return token;
        }
    }

    private static final class CountingTokenSource implements Iterator<Token> {

        private final Iterator<Token> delegate;
        private int provided;

        private CountingTokenSource(Iterator<Token> delegate) {
            this.delegate = delegate;
            this.provided = 0;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public Token next() {
            Token token = delegate.next();
            provided = provided + 1;
            return token;
        }
    }

    private static Token token(String value, int column) {
        Position position = new Position(1, column);
        return new Token(TokenType.IDENTIFIER, value, new Range(position, position));
    }

    private static TokenBuffer bufferOf(Token... tokens) {
        return new TokenBuffer(new ListTokenSource(List.of(tokens)));
    }

    @Test
    void lookaheadDoesNotConsume() {
        TokenBuffer buffer = bufferOf(token("a", 1), token("b", 2));

        Optional<Token> first = buffer.lookahead(0);
        Optional<Token> firstAgain = buffer.lookahead(0);
        Optional<Token> second = buffer.lookahead(1);
        Optional<Token> firstAfterDeeperLook = buffer.lookahead(0);

        assertEquals(Optional.of(token("a", 1)), first);
        assertEquals(first, firstAgain);
        assertEquals(Optional.of(token("b", 2)), second);
        assertEquals(first, firstAfterDeeperLook);
    }

    @Test
    void lookaheadBeyondTheEndOfTheStreamReturnsEmpty() {
        TokenBuffer buffer = bufferOf(token("a", 1), token("b", 2));

        assertEquals(Optional.empty(), buffer.lookahead(2));
        assertEquals(Optional.empty(), buffer.lookahead(10));
        assertEquals(Optional.of(token("b", 2)), buffer.lookahead(1));

        TokenBuffer empty = bufferOf();

        assertEquals(Optional.empty(), empty.lookahead(0));
    }

    @Test
    void commitShiftsTheWindow() {
        TokenBuffer buffer = bufferOf(token("a", 1), token("b", 2), token("c", 3));

        buffer.commit(1);

        assertEquals(Optional.of(token("b", 2)), buffer.lookahead(0));
        assertEquals(Optional.of(token("c", 3)), buffer.lookahead(1));

        buffer.commit(2);

        assertEquals(Optional.empty(), buffer.lookahead(0));
    }

    @Test
    void commitBeyondTheEndOfTheStreamThrows() {
        TokenBuffer buffer = bufferOf(token("a", 1), token("b", 2));

        assertThrows(NoSuchElementException.class, () -> buffer.commit(3));
    }

    @Test
    void sourceProvidesOnlyWhatWasLookedAhead() {
        CountingTokenSource source =
                new CountingTokenSource(
                        new ListTokenSource(
                                List.of(
                                        token("a", 1),
                                        token("b", 2),
                                        token("c", 3),
                                        token("d", 4))));
        TokenBuffer buffer = new TokenBuffer(source);

        buffer.lookahead(0);
        assertEquals(1, source.provided);

        buffer.lookahead(0);
        assertEquals(1, source.provided);

        buffer.lookahead(2);
        assertEquals(3, source.provided);

        buffer.commit(3);
        assertEquals(3, source.provided);
    }

    @Test
    void commitWithoutLookaheadPullsExactlyTheCommittedTokens() {
        CountingTokenSource source =
                new CountingTokenSource(
                        new ListTokenSource(List.of(token("a", 1), token("b", 2), token("c", 3))));
        TokenBuffer buffer = new TokenBuffer(source);

        buffer.commit(2);

        assertEquals(2, source.provided);
        assertEquals(Optional.of(token("c", 3)), buffer.lookahead(0));
    }

    @Test
    void negativeArgumentsAreRejected() {
        TokenBuffer buffer = bufferOf(token("a", 1));

        Executable negativeLookahead = () -> buffer.lookahead(-1);
        Executable negativeCommit = () -> buffer.commit(-1);

        assertThrows(IllegalArgumentException.class, negativeLookahead);
        assertThrows(IllegalArgumentException.class, negativeCommit);
        assertTrue(buffer.lookahead(0).isPresent());
    }
}
