package org.printscript.semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// la única tabla de tipos del sistema: qué nombres existen y con qué tipo
// es inmutable, igual que Environment: cada cambio devuelve una tabla nueva
public final class SymbolTable {

    private final Map<String, Type> types;

    public SymbolTable() {
        this(Map.of());
    }

    private SymbolTable(Map<String, Type> types) {
        this.types = types;
    }

    // vacío si el nombre ya estaba declarado: declarar dos veces es un error semántico
    public Optional<SymbolTable> declare(String name, Type type) {
        if (types.containsKey(name)) {
            return Optional.empty();
        }
        Map<String, Type> updated = new HashMap<>(types);
        updated.put(name, type);
        return Optional.of(new SymbolTable(Map.copyOf(updated)));
    }

    public Optional<Type> read(String name) {
        return Optional.ofNullable(types.get(name));
    }
}
