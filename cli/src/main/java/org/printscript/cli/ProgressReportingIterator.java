package org.printscript.cli;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

// reporta el progreso del parseo estimando el total consumido en bytes contra el tamaño del archivo
final class ProgressReportingIterator implements Iterator<String> {

    private static final int MAX_PERCENT = 100;

    private final Iterator<String> delegate;
    private final long totalBytes;
    private final PrintStream progressOut;
    private long consumedBytes;
    private int lastReportedPercent = -1;

    ProgressReportingIterator(Iterator<String> delegate, long totalBytes, PrintStream progressOut) {
        this.delegate = delegate;
        this.totalBytes = totalBytes;
        this.progressOut = progressOut;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public String next() {
        if (!delegate.hasNext()) {
            throw new NoSuchElementException();
        }
        String line = delegate.next();
        consumedBytes += line.getBytes(StandardCharsets.UTF_8).length + 1;
        reportProgressIfChanged();
        return line;
    }

    private void reportProgressIfChanged() {
        int percent = percentComplete();
        if (percent != lastReportedPercent) {
            progressOut.print("\rProgreso: " + percent + "%");
            if (percent >= MAX_PERCENT) {
                progressOut.println();
            }
            lastReportedPercent = percent;
        }
    }

    private int percentComplete() {
        if (totalBytes <= 0) {
            return MAX_PERCENT;
        }
        return (int) Math.min(MAX_PERCENT, consumedBytes * MAX_PERCENT / totalBytes);
    }
}
