package org.printscript.semantic;

import java.util.Optional;
import org.printscript.common.ScopedValues;

// Tabla de nombres y tipos. La política de redeclaración pertenece a ScopedValues.
public final class SymbolTable {

    private final ScopedValues<Symbol> symbols;

    public SymbolTable() {
        this(new ScopedValues<>());
    }

    private SymbolTable(ScopedValues<Symbol> symbols) {
        this.symbols = symbols;
    }

    public SymbolTable openScope() {
        return new SymbolTable(symbols.openScope());
    }

    public SymbolTable closeScope() {
        return new SymbolTable(symbols.closeScope());
    }

    public Optional<SymbolTable> declare(String name, Symbol symbol) {
        return symbols.declare(name, symbol).map(SymbolTable::new);
    }

    public Optional<Symbol> read(String name) {
        return symbols.read(name);
    }
}
