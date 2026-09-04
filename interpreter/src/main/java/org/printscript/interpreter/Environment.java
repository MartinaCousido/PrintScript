package org.printscript.interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// tabla de variables
// es la única memoria del programa mientras corre
// es inmutable, cada cambio devuelve una tabla nueva
public final class Environment {

    private final Map<String, Variable> variables;

    public Environment() {
        this(Map.of());
    }

    private Environment(Map<String, Variable> variables) {
        this.variables = variables;
    }

    public Optional<Environment> declare(
            String name, String declaredType, Object value, boolean immutable) {
        if (variables.containsKey(name)) {
            return Optional.empty();
        }
        return Optional.of(withVariable(name, new Variable(declaredType, value, immutable)));
    }

    // vacío también cuando el nombre existe pero es una constante: reasignarla no se
    // puede. semantic ya lo rechaza antes; acá queda como red de seguridad.
    public Optional<Environment> assign(String name, Object value) {
        return read(name)
                .filter(existing -> !existing.immutable())
                .map(
                        existing ->
                                withVariable(
                                        name, new Variable(existing.declaredType(), value, false)));
    }

    public Optional<Variable> read(String name) {
        return Optional.ofNullable(variables.get(name));
    }

    private Environment withVariable(String name, Variable variable) {
        Map<String, Variable> updated = new HashMap<>(variables);
        updated.put(name, variable);
        return new Environment(Map.copyOf(updated));
    }
}
