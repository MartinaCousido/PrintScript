package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;

class LexerTest {

    private static final class RecordingLines implements Iterator<String> {

        private final List<String> lines;
        private int index;
        private int requested;

        private RecordingLines(List<String> lines) {
            this.lines = lines;
            this.index = 0;
            this.requested = 0;
        }

        @Override
        public boolean hasNext() {
            return index < lines.size();
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String line = lines.get(index);
            index = index + 1;
            requested = requested + 1;
            return line;
        }

        private int requestedCount() {
            return requested;
        }
    }

    private static Lexer lexerOf(String... lines) {
        return new Lexer(List.of(lines).iterator(), List.of());
    }

    @Test
    void inputWithoutContentProducesNoTokens() {
        assertFalse(lexerOf().hasNext());
        assertFalse(lexerOf("", "   ", "\t \t").hasNext());
    }

    @Test
    void positionAdvancesToTheNextLine() {
        Lexer lexer = lexerOf("a", "  b");

        lexer.getNext();
        Token second = lexer.getNext();

        assertEquals(new Range(new Position(2, 3), new Position(2, 3)), second.range());
    }

    @Test
    void theNextLineIsNotRequestedUntilTheCurrentOneIsExhausted() {
        RecordingLines lines = new RecordingLines(List.of("a b", "c"));
        Lexer lexer = new Lexer(lines, List.of());

        assertEquals(0, lines.requestedCount());

        lexer.getNext();
        assertEquals(1, lines.requestedCount());

        lexer.getNext();
        assertEquals(1, lines.requestedCount());

        assertTrue(lexer.hasNext());
        assertEquals(2, lines.requestedCount());
    }
}
