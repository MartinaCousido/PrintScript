package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Version;
import org.printscript.runtime.OutputPrinter;
import org.printscript.runtime.RuntimePorts;

// El cierre del hito: las cinco features de 1.1 juntas, la compuerta de versión, los
// errores que el modo Validation tiene que dar sin ejecutar nada, y la cota de memoria
// que asumimos al cortar por profundidad de llaves.
class Version11SuiteEndToEndTest {

    private static final class RecordingPrinter implements OutputPrinter {

        private final List<String> lines = new ArrayList<>();

        @Override
        public void println(String line) {
            lines.add(line);
        }

        @Override
        public void print(String text) {
            lines.add(text);
        }
    }

    // cuenta cuántas líneas del fuente se pidieron hasta ahora
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

    private static RuntimePorts ports(
            OutputPrinter printer, List<String> input, Map<String, String> environment) {
        Deque<String> pending = new ArrayDeque<>(input);
        return new RuntimePorts(
                printer,
                () -> pending.isEmpty() ? Optional.empty() : Optional.of(pending.removeFirst()),
                name -> Optional.ofNullable(environment.get(name)));
    }

    private static List<Failure<?>> validate(Version version, String... lines) {
        return new ValidationRunner().validate(List.of(lines).iterator(), version);
    }

    private static void assertRange(Failure<?> failure, int line, int startColumn, int endColumn) {
        assertEquals(new Position(line, startColumn), failure.range().start());
        assertEquals(new Position(line, endColumn), failure.range().end());
    }

    // --- 1.1 es un superconjunto de 1.0 ---

    @Test
    void theConsignaExamplesGiveTheSameOutputInBothVersions() {
        String[][] ejemplos = {
            {
                "let name: string = \"Joe\";",
                "let lastName: string = \"Doe\";",
                "println(name + \" \" + lastName);"
            },
            {
                "let a: number = 12;",
                "let b: number = 4;",
                "let c: number = a / b;",
                "println(\"Result: \" + c);"
            },
            {
                "let a: number = 12;",
                "let b: number = 4;",
                "a = a / b;",
                "println(\"Result: \" + a);"
            }
        };
        List<String> esperado = List.of("Joe Doe", "Result: 3", "Result: 3");

        for (int index = 0; index < ejemplos.length; index = index + 1) {
            RecordingPrinter conDiez = new RecordingPrinter();
            RecordingPrinter conOnce = new RecordingPrinter();
            new ExecutionRunner().run(List.of(ejemplos[index]).iterator(), conDiez, Version.V10);
            new ExecutionRunner().run(List.of(ejemplos[index]).iterator(), conOnce, Version.V11);

            assertEquals(List.of(esperado.get(index)), conDiez.lines);
            assertEquals(conDiez.lines, conOnce.lines);
        }
    }

    // --- el camino feliz con las cinco features juntas ---

    private static final String[] PROGRAMA_11 = {
        "const saludo: string = \"Hola\";",
        "let nombre: string = readInput(\"nombre: \");",
        "let umbral: number = readEnv(\"UMBRAL\");",
        "let corto: boolean = true;",
        "if (corto) {",
        "  let largo: boolean = false;",
        "  if (largo) {",
        "    println(\"nunca\");",
        "  } else if (corto) {",
        "    println(saludo + \" \" + nombre);",
        "  } else {",
        "    println(\"tampoco\");",
        "  }",
        "} else {",
        "  println(\"no\");",
        "}",
        "println(umbral + 1);"
    };

    @Test
    void theFiveFeaturesWorkTogether() {
        RecordingPrinter printer = new RecordingPrinter();

        List<Failure<?>> failures =
                new ExecutionRunner()
                        .run(
                                List.of(PROGRAMA_11).iterator(),
                                ports(printer, List.of("Joe"), Map.of("UMBRAL", "41")),
                                Version.V11);

        assertEquals(List.of(), failures);
        assertEquals(List.of("nombre: ", "Hola Joe", "42"), printer.lines);
    }

    @Test
    void thatSameProgramIsRejectedByVersion10() {
        List<Failure<?>> failures = validate(Version.V10, PROGRAMA_11);

        assertFalse(failures.isEmpty());
    }

    // --- lo que Validation tiene que detectar sin ejecutar nada ---

    @Test
    void aWrongDeclaredTypeIsReportedWithItsPosition() {
        Failure<?> failure = validate(Version.V11, "let x: number = \"hola\";").getFirst();

        assertEquals("No se puede asignar string a una variable number", failure.message());
        assertRange(failure, 1, 17, 22);
    }

    @Test
    void aNonBooleanConditionIsReportedWithItsPosition() {
        Failure<?> failure = validate(Version.V11, "if (5) { println(1); }").getFirst();

        assertEquals("La condición de un if tiene que ser boolean", failure.message());
        assertRange(failure, 1, 5, 5);
    }

    @Test
    void reassigningAConstantIsReportedWithItsPosition() {
        Failure<?> failure = validate(Version.V11, "const c: number = 1;", "c = 2;").getFirst();

        assertEquals("No se puede reasignar la constante c", failure.message());
        assertRange(failure, 2, 1, 6);
    }

    @Test
    void aConstantWithoutValueIsReportedWithItsPosition() {
        Failure<?> failure = validate(Version.V11, "const c: number;").getFirst();

        assertEquals("Una constante tiene que declararse con un valor", failure.message());
        assertRange(failure, 1, 1, 16);
    }

    @Test
    void aNameUsedOutsideItsBlockIsReportedWithItsPosition() {
        Failure<?> failure =
                validate(Version.V11, "if (true) { let x: number = 1; }", "println(x);").getFirst();

        assertEquals("Variable no declarada: x", failure.message());
        assertRange(failure, 2, 9, 9);
    }

    @Test
    void everyErrorOfTheProgramIsCollected() {
        List<Failure<?>> failures =
                validate(
                        Version.V11,
                        "let a: number = \"uno\";",
                        "println(\"ok\");",
                        "let b: number = \"dos\";");

        assertEquals(2, failures.size());
    }

    // --- la cota de memoria del streaming ---

    @Test
    void theSourceIsNotHeldWholeWhenThereAreBlocks() {
        // memoria acotada al bloque más grande, no al programa: al terminar el primer if
        // el fuente todavía no se leyó entero
        List<String> program = new ArrayList<>();
        program.add("if (true) {");
        program.add("  println(\"primero\");");
        program.add("}");
        for (int index = 0; index < 200; index = index + 1) {
            program.add("println(\"linea " + index + "\");");
        }
        CountingIterator lines = new CountingIterator(program);
        RecordingPrinter printer = new RecordingPrinter();

        new ExecutionRunner().run(lines, printer, Version.V11);

        assertEquals(201, printer.lines.size());
        assertEquals(program.size(), lines.consumed);
    }

    @Test
    void aBlockIsReadOnlyUpToItsClosingBrace() {
        List<String> program = new ArrayList<>();
        program.add("if (true) {");
        program.add("  println(\"adentro\");");
        program.add("}");
        for (int index = 0; index < 100; index = index + 1) {
            program.add("println(\"linea " + index + "\");");
        }
        CountingIterator lines = new CountingIterator(program);
        RecordingPrinter printer = new RecordingPrinter();

        // se para apenas el if imprimió: el resto del fuente no debería estar leído
        List<Failure<?>> ignored =
                new ExecutionRunner().run(new StopAfterFirst(lines), printer, Version.V11);

        assertTrue(
                lines.consumed < program.size(),
                "se leyó " + lines.consumed + " de " + program.size());
        assertEquals(List.of(), ignored);
    }

    // corta el fuente después de la primera línea que sigue al bloque, para poder medir
    // cuánto se leyó por adelantado
    private static final class StopAfterFirst implements Iterator<String> {

        private final CountingIterator source;
        private int delivered;

        private StopAfterFirst(CountingIterator source) {
            this.source = source;
        }

        @Override
        public boolean hasNext() {
            return delivered < 4 && source.hasNext();
        }

        @Override
        public String next() {
            delivered = delivered + 1;
            return source.next();
        }
    }
}
