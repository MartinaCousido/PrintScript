package org.printscript.interpreter;

import java.util.Optional;
import org.printscript.common.ScopedValues;

// Estado de variables durante la ejecución. La inmutabilidad de const se aplica acá.
public final class Environment {

    private final ScopedValues<Variable> variables;

    public Environment() {
        this(new ScopedValues<>());
    }

    private Environment(ScopedValues<Variable> variables) {
        this.variables = variables;
    }

    public Environment openScope() {
        return new Environment(variables.openScope());
    }

    public Environment closeScope() {
        return new Environment(variables.closeScope());
    }

    public Optional<Environment> declare(
            String name, String declaredType, Object value, boolean immutable) {
        return variables
                .declare(name, new Variable(declaredType, value, immutable))
                .map(Environment::new);
    }

    public Optional<Environment> assign(String name, Object value) {
        return variables
                .read(name)
                .filter(existing -> !existing.immutable())
                .flatMap(
                        existing ->
                                variables.replace(
                                        name, new Variable(existing.declaredType(), value, false)))
                .map(Environment::new);
    }

    public Optional<Variable> read(String name) {
        return variables.read(name);
    }
}
