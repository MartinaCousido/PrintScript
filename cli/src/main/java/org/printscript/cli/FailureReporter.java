package org.printscript.cli;

import java.io.PrintStream;
import java.util.List;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;

// formatea los Failure (mensaje + Range con fila/columna de inicio y fin) que pide
// la consigna cuando hay errores sintácticos o semánticos
final class FailureReporter {

    private final PrintStream target;

    FailureReporter(PrintStream target) {
        this.target = target;
    }

    void report(List<Failure<?>> failures) {
        for (Failure<?> failure : failures) {
            target.println(format(failure));
        }
    }

    private static String format(Failure<?> failure) {
        Range range = failure.range();
        Position start = range.start();
        Position end = range.end();
        return "error: "
                + failure.message()
                + " (línea "
                + start.line()
                + ", columna "
                + start.column()
                + " a línea "
                + end.line()
                + ", columna "
                + end.column()
                + ")";
    }
}
