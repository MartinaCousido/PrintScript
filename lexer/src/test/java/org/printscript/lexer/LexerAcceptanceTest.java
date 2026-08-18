package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.printscript.lexer.providers.LexerVersionProvider;
import org.printscript.token.Token;

class LexerAcceptanceTest {

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

        private int requested() {
            return requested;
        }
    }

    private static Lexer lexerOf(Iterator<String> lines) {
        return new LexerVersionProvider().providerFor("1.0").provide(lines);
    }

    private static List<String> tokenize(String... lines) {
        Lexer lexer = lexerOf(List.of(lines).iterator());
        List<String> described = new ArrayList<>();
        while (lexer.hasNext()) {
            described.add(describe(lexer.getNext()));
        }
        return described;
    }

    private static String describe(Token token) {
        return token.type()
            + "|" + token.value()
            + "|" + token.range().start().line() + ":" + token.range().start().column()
            + "-" + token.range().end().line() + ":" + token.range().end().column();
    }

    @Test
    void tokenizesTheExampleOfTheAssignment() {
        List<String> tokens = tokenize(
            "let x: number;",
            "let a: number = 12;",
            "let b: number = 4;",
            "let c: number = a / b;",
            "println(\"Result: \" + c);");

        assertEquals(
            List.of(
                "KEYWORD|let|1:1-1:3",
                "IDENTIFIER|x|1:5-1:5",
                "PUNCTUATION|:|1:6-1:6",
                "KEYWORD|number|1:8-1:13",
                "PUNCTUATION|;|1:14-1:14",
                "KEYWORD|let|2:1-2:3",
                "IDENTIFIER|a|2:5-2:5",
                "PUNCTUATION|:|2:6-2:6",
                "KEYWORD|number|2:8-2:13",
                "OPERATOR|=|2:15-2:15",
                "NUMBER|12|2:17-2:18",
                "PUNCTUATION|;|2:19-2:19",
                "KEYWORD|let|3:1-3:3",
                "IDENTIFIER|b|3:5-3:5",
                "PUNCTUATION|:|3:6-3:6",
                "KEYWORD|number|3:8-3:13",
                "OPERATOR|=|3:15-3:15",
                "NUMBER|4|3:17-3:17",
                "PUNCTUATION|;|3:18-3:18",
                "KEYWORD|let|4:1-4:3",
                "IDENTIFIER|c|4:5-4:5",
                "PUNCTUATION|:|4:6-4:6",
                "KEYWORD|number|4:8-4:13",
                "OPERATOR|=|4:15-4:15",
                "IDENTIFIER|a|4:17-4:17",
                "OPERATOR|/|4:19-4:19",
                "IDENTIFIER|b|4:21-4:21",
                "PUNCTUATION|;|4:22-4:22",
                "KEYWORD|println|5:1-5:7",
                "PUNCTUATION|(|5:8-5:8",
                "STRING|Result: |5:9-5:18",
                "OPERATOR|+|5:20-5:20",
                "IDENTIFIER|c|5:22-5:22",
                "PUNCTUATION|)|5:23-5:23",
                "PUNCTUATION|;|5:24-5:24"),
            tokens);
    }

    @Test
    void tokenizesBothQuoteStylesInTheSameProgram() {
        List<String> tokens = tokenize("println('one' + \"two\");");

        assertEquals(
            List.of(
                "KEYWORD|println|1:1-1:7",
                "PUNCTUATION|(|1:8-1:8",
                "STRING|one|1:9-1:13",
                "OPERATOR|+|1:15-1:15",
                "STRING|two|1:17-1:21",
                "PUNCTUATION|)|1:22-1:22",
                "PUNCTUATION|;|1:23-1:23"),
            tokens);
    }

    @Test
    void theRangesReflectTheLineOfEachTokenInAMultiLineProgram() {
        List<String> tokens = tokenize("println(1);", "println(2);");

        assertEquals(
            List.of(
                "KEYWORD|println|1:1-1:7",
                "PUNCTUATION|(|1:8-1:8",
                "NUMBER|1|1:9-1:9",
                "PUNCTUATION|)|1:10-1:10",
                "PUNCTUATION|;|1:11-1:11",
                "KEYWORD|println|2:1-2:7",
                "PUNCTUATION|(|2:8-2:8",
                "NUMBER|2|2:9-2:9",
                "PUNCTUATION|)|2:10-2:10",
                "PUNCTUATION|;|2:11-2:11"),
            tokens);
    }

    @Test
    void anInvalidCharacterProducesUnknownAndTheRestIsTokenizedAnyway() {
        List<String> tokens = tokenize("let a$: number = 1;");

        assertEquals(
            List.of(
                "KEYWORD|let|1:1-1:3",
                "IDENTIFIER|a|1:5-1:5",
                "UNKNOWN|$|1:6-1:6",
                "PUNCTUATION|:|1:7-1:7",
                "KEYWORD|number|1:9-1:14",
                "OPERATOR|=|1:16-1:16",
                "NUMBER|1|1:18-1:18",
                "PUNCTUATION|;|1:19-1:19"),
            tokens);
    }

    @Test
    void theLinesAreConsumedOnDemandAndNotAllWhenTheLexerIsBuilt() {
        RecordingLines lines = new RecordingLines(List.of("let a: number = 1;", "println(a);"));

        Lexer lexer = lexerOf(lines);
        assertEquals(0, lines.requested());

        for (int consumed = 0; consumed < 7; consumed = consumed + 1) {
            lexer.getNext();
        }
        assertEquals(1, lines.requested());

        assertTrue(lexer.hasNext());
        assertEquals(2, lines.requested());
    }
}
