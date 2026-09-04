package org.printscript.interpreter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.printscript.runtime.RuntimePorts;

// puertos en memoria para los tests: nada de stdin ni del entorno real de la máquina
final class Ports {

    private Ports() {}

    static RuntimePorts of(org.printscript.runtime.OutputPrinter printer) {
        return new RuntimePorts(printer, Optional::empty, name -> Optional.empty());
    }

    static RuntimePorts of(
            org.printscript.runtime.OutputPrinter printer,
            List<String> input,
            Map<String, String> environment) {
        Deque<String> pending = new ArrayDeque<>(input);
        return new RuntimePorts(
                printer,
                () -> pending.isEmpty() ? Optional.empty() : Optional.of(pending.removeFirst()),
                name -> Optional.ofNullable(environment.get(name)));
    }
}
