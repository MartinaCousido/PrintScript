package org.printscript.semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// la única tabla de tipos del sistema: qué nombres existen, con qué tipo y si son
// reasignables. Es inmutable, igual que Environment: cada cambio devuelve una nueva.
public final class SymbolTable {

    private final Map<String, Symbol> symbols;

    public SymbolTable() {
        this(Map.of());
    }

    private SymbolTable(Map<String, Symbol> symbols) {
        this.symbols = symbols;
    }

    // vacío si el nombre ya estaba declarado: declarar dos veces es un error semántico
    public Optional<SymbolTable> declare(String name, Symbol symbol) {
        if (symbols.containsKey(name)) {
            return Optional.empty();
        }
        Map<String, Symbol> updated = new HashMap<>(symbols);
        updated.put(name, symbol);
        return Optional.of(new SymbolTable(Map.copyOf(updated)));
    }

    public Optional<Symbol> read(String name) {
        return Optional.ofNullable(symbols.get(name));
    }
}
