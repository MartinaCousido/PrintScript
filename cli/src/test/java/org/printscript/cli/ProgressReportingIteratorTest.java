package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ProgressReportingIteratorTest {

    private static final class CountingIterator implements Iterator<String> {
        private final Iterator<String> delegate;
        private int nextCalls;

        CountingIterator(List<String> lines) {
            this.delegate = lines.iterator();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public String next() {
            nextCalls++;
            return delegate.next();
        }
    }

    private static long totalBytesOf(List<String> lines) {
        return lines.stream()
                .mapToLong(line -> line.getBytes(StandardCharsets.UTF_8).length + 1)
                .sum();
    }

    @Test
    void consumesEachLineExactlyOnceFromTheDelegate() {
        List<String> lines = List.of("aaaa", "bb", "cccccccc");
        CountingIterator counting = new CountingIterator(lines);
        Iterator<String> iterator =
                new ProgressReportingIterator(
                        counting,
                        totalBytesOf(lines),
                        new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));

        while (iterator.hasNext()) {
            iterator.next();
        }

        assertEquals(lines.size(), counting.nextCalls);
    }

    @Test
    void reportsProgressUpToOneHundredPercent() {
        List<String> lines = List.of("a".repeat(10), "b".repeat(10), "c".repeat(80));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Iterator<String> iterator =
                new ProgressReportingIterator(
                        lines.iterator(),
                        totalBytesOf(lines),
                        new PrintStream(buffer, true, StandardCharsets.UTF_8));

        while (iterator.hasNext()) {
            iterator.next();
        }

        assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("100%"));
    }

    @Test
    void clampsAtOneHundredPercentWhenTotalBytesIsSmallerThanConsumed() {
        List<String> lines = List.of("línea larga de más de diez bytes");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Iterator<String> iterator =
                new ProgressReportingIterator(
                        lines.iterator(), 5, new PrintStream(buffer, true, StandardCharsets.UTF_8));

        String line = iterator.next();

        assertEquals(lines.getFirst(), line);
        assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("100%"));
    }

    @Test
    void nextThrowsWhenExhausted() {
        Iterator<String> iterator =
                new ProgressReportingIterator(
                        List.<String>of().iterator(),
                        10,
                        new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));

        assertThrows(NoSuchElementException.class, iterator::next);
    }
}
