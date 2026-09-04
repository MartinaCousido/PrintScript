package org.printscript.semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// la única tabla de tipos del sistema: qué nombres existen, con qué tipo y si son
// reasignables. Es inmutable, igual que Environment: cada cambio devuelve una nueva.
// Los frames son el scope: se abre uno al entrar a un Block y se cierra al salir, y la
// resolución de un nombre va del más interno hacia afuera.
public final class SymbolTable {

    // cerrar el scope de más afuera no tiene sentido: no hay a dónde volver
    private static final int OUTERMOST_ONLY = 1;

    private final List<Map<String, Symbol>> frames;

    public SymbolTable() {
        this(List.of(Map.of()));
    }

    private SymbolTable(List<Map<String, Symbol>> frames) {
        this.frames = frames;
    }

    public SymbolTable openScope() {
        List<Map<String, Symbol>> opened = new ArrayList<>(frames);
        opened.add(Map.of());
        return new SymbolTable(List.copyOf(opened));
    }

    public SymbolTable closeScope() {
        if (frames.size() == OUTERMOST_ONLY) {
            return this;
        }
        return new SymbolTable(List.copyOf(frames.subList(0, frames.size() - 1)));
    }

    // vacío si el nombre ya estaba declarado en ESTE scope; declararlo de nuevo en uno
    // más interno es sombra, y es válido
    public Optional<SymbolTable> declare(String name, Symbol symbol) {
        if (innermost().containsKey(name)) {
            return Optional.empty();
        }
        Map<String, Symbol> updated = new HashMap<>(innermost());
        updated.put(name, symbol);
        return Optional.of(withInnermost(updated));
    }

    public Optional<Symbol> read(String name) {
        for (int index = frames.size() - 1; index >= 0; index = index - 1) {
            Symbol found = frames.get(index).get(name);
            if (found != null) {
                return Optional.of(found);
            }
        }
        return Optional.empty();
    }

    private Map<String, Symbol> innermost() {
        return frames.getLast();
    }

    private SymbolTable withInnermost(Map<String, Symbol> updated) {
        List<Map<String, Symbol>> replaced = new ArrayList<>(frames);
        replaced.set(replaced.size() - 1, Map.copyOf(updated));
        return new SymbolTable(List.copyOf(replaced));
    }
}
