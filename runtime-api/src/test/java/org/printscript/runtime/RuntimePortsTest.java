package org.printscript.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RuntimePortsTest {

    private static final class Recording implements OutputPrinter {

        private final List<String> written = new ArrayList<>();

        @Override
        public void println(String line) {
            written.add(line + "\n");
        }

        @Override
        public void print(String text) {
            written.add(text);
        }
    }

    @Test
    void theThreePortsTravelTogether() {
        Recording printer = new Recording();
        Map<String, String> environment = Map.of("PORT", "8080");
        RuntimePorts ports =
                new RuntimePorts(
                        printer,
                        () -> Optional.of("una línea"),
                        name -> Optional.ofNullable(environment.get(name)));

        ports.printer().print("prompt: ");
        ports.printer().println("salida");

        assertEquals(List.of("prompt: ", "salida\n"), printer.written);
        assertEquals(Optional.of("una línea"), ports.input().readLine());
        assertEquals(Optional.of("8080"), ports.env().read("PORT"));
        assertTrue(ports.env().read("NO_ESTA").isEmpty());
    }

    @Test
    void anInputThatRanOutIsAnEmptyOptionalAndNotAnException() {
        // quedarse sin entrada es una situación normal: viaja como dato
        RuntimePorts ports =
                new RuntimePorts(new Recording(), Optional::empty, name -> Optional.empty());

        assertTrue(ports.input().readLine().isEmpty());
    }
}
