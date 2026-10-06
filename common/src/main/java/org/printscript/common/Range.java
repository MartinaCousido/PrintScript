package org.printscript.common;

import java.util.Objects;

// desde que posición hasta que posición está algo en el archivo
public record Range(Position start, Position end) {

    public Range {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        boolean earlierLine = end.line() < start.line();
        boolean earlierColumnOnSameLine =
                end.line() == start.line() && end.column() < start.column();
        if (earlierLine || earlierColumnOnSameLine) {
            throw new IllegalArgumentException("Range end must not precede start");
        }
    }
}
