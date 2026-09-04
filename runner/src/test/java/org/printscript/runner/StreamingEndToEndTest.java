package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.printscript.common.Result;
import org.printscript.common.Version;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.InterpreterFactory;
import org.printscript.lexer.Lexer;
import org.printscript.lexer.LexerFactory;
import org.printscript.parser.Parser;
import org.printscript.parser.ParserFactory;
import org.printscript.parser.TokenBuffer;
import org.printscript.runtime.OutputPrinter;
import org.printscript.runtime.RuntimePorts;

class StreamingEndToEndTest {

    private static final class CountingIterator implements Iterator<String> {

        private final List<String> lines;
        private int index;
        private int consumed;

        private CountingIterator(List<String> lines) {
            this.lines = lines;
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
            consumed = consumed + 1;
            String line = lines.get(index);
            index = index + 1;
            return line;
        }
    }

    private static final class RecordingPrinter implements OutputPrinter {

        private final CountingIterator source;
        private final List<Integer> consumedAtEachPrint = new ArrayList<>();

        private RecordingPrinter(CountingIterator source) {
            this.source = source;
        }

        @Override
        public void println(String line) {
            consumedAtEachPrint.add(source.consumed);
        }

        @Override
        public void print(String text) {
            consumedAtEachPrint.add(source.consumed);
        }
    }

    private static Interpreter buildChain(CountingIterator lines, RecordingPrinter printer) {
        Lexer lexer = LexerFactory.create(lines, Version.V10);
        TokenBuffer buffer = new TokenBuffer(lexer);
        Parser parser = ParserFactory.create(buffer, Version.V10);
        return InterpreterFactory.create(
                parser,
                new RuntimePorts(
                        printer, java.util.Optional::empty, name -> java.util.Optional.empty()),
                Version.V10);
    }

    @Test
    void buildingTheChainDoesNotConsumeAnyLine() {
        CountingIterator lines = new CountingIterator(List.of("println(\"uno\");"));
        RecordingPrinter printer = new RecordingPrinter(lines);

        buildChain(lines, printer);

        assertEquals(0, lines.consumed);
    }

    @Test
    void linesAreRequestedOneAtATimeAsExecutionAdvances() {
        List<String> program =
                List.of(
                        "println(\"uno\");",
                        "println(\"dos\");",
                        "println(\"tres\");",
                        "println(\"cuatro\");");
        CountingIterator lines = new CountingIterator(program);
        RecordingPrinter printer = new RecordingPrinter(lines);
        Interpreter interpreter = buildChain(lines, printer);

        Result<?> first = interpreter.getNext();

        assertTrue(lines.consumed < program.size(), "consumed=" + lines.consumed);
        assertEquals(1, lines.consumed);
        assertNotNull(first);
    }

    @Test
    void theFirstStatementsOutputIsProducedBeforeTheLastSourceLineIsRequested() {
        List<String> program =
                List.of(
                        "println(\"primera\");",
                        "println(\"segunda\");",
                        "println(\"tercera\");",
                        "println(\"ultima\");");
        CountingIterator lines = new CountingIterator(program);
        RecordingPrinter printer = new RecordingPrinter(lines);
        Interpreter interpreter = buildChain(lines, printer);

        while (interpreter.hasNext()) {
            interpreter.getNext();
        }

        assertEquals(program.size(), printer.consumedAtEachPrint.size());
        int consumedAtFirstPrint = printer.consumedAtEachPrint.getFirst();
        assertTrue(
                consumedAtFirstPrint < program.size(),
                "consumedAtFirstPrint=" + consumedAtFirstPrint);
        assertEquals(1, consumedAtFirstPrint);
    }
}
