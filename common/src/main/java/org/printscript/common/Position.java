package org.printscript.common;

// una fila y una columna, las dos empiezan en 1, no en 0
public record Position(int line, int column) {

    public Position {
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("Line and column must start at 1");
        }
    }
}
