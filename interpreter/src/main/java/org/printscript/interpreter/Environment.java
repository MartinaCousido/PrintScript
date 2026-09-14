package org.printscript.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// tabla de variables
// es la única memoria del programa mientras corre
// los frames son el scope: se abre uno al entrar a un Block y se cierra al salir, igual
// que en la tabla de tipos de semantic, y en el mismo lugar
public final class Environment {

    // cerrar el scope de más afuera no tiene sentido: no hay a dónde volver
    private static final int OUTERMOST_ONLY = 1;

    private final List<Map<String, Variable>> frames;

    public Environment() {
        this(List.of(Map.of()));
    }

    private Environment(List<Map<String, Variable>> frames) {
        this.frames = frames;
    }

    public Environment openScope() {
        List<Map<String, Variable>> opened = new ArrayList<>(frames);
        opened.add(Map.of());
        return new Environment(List.copyOf(opened));
    }

    public Environment closeScope() {
        if (frames.size() == OUTERMOST_ONLY) {
            return this;
        }
        return new Environment(List.copyOf(frames.subList(0, frames.size() - 1)));
    }

    // vacío si el nombre ya existe en ESTE scope
    public Optional<Environment> declare(
            String name, String declaredType, Object value, boolean immutable) {
        if (innermost().containsKey(name)) {
            return Optional.empty();
        }
        Map<String, Variable> updated = new HashMap<>(innermost());
        updated.put(name, new Variable(declaredType, value, immutable));
        return Optional.of(withInnermost(updated));
    }

    // vacío también cuando el nombre existe pero es una constante: reasignarla no se
    // puede. semantic ya lo rechaza antes; acá queda como red de seguridad.
    public Optional<Environment> assign(String name, Object value) {
        int frame = frameOf(name);
        if (frame < 0 || frames.get(frame).get(name).immutable()) {
            return Optional.empty();
        }
        Variable existing = frames.get(frame).get(name);
        Map<String, Variable> updated = new HashMap<>(frames.get(frame));
        updated.put(name, new Variable(existing.declaredType(), value, false));
        List<Map<String, Variable>> replaced = new ArrayList<>(frames);
        replaced.set(frame, Map.copyOf(updated));
        return Optional.of(new Environment(List.copyOf(replaced)));
    }

    public Optional<Variable> read(String name) {
        int frame = frameOf(name);
        return frame < 0 ? Optional.empty() : Optional.of(frames.get(frame).get(name));
    }

    private int frameOf(String name) {
        for (int index = frames.size() - 1; index >= 0; index = index - 1) {
            if (frames.get(index).containsKey(name)) {
                return index;
            }
        }
        return -1;
    }

    private Map<String, Variable> innermost() {
        return frames.getLast();
    }

    private Environment withInnermost(Map<String, Variable> updated) {
        List<Map<String, Variable>> replaced = new ArrayList<>(frames);
        replaced.set(replaced.size() - 1, Map.copyOf(updated));
        return new Environment(List.copyOf(replaced));
    }
}
