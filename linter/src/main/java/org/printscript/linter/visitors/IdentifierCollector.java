package org.printscript.linter.visitors;

import org.printscript.ast.Identifier;

/** Collects Identifier nodes in source traversal order, including nested blocks and reads. */
public final class IdentifierCollector extends NodeCollector<Identifier> {
    public IdentifierCollector() {
        super(Identifier.class);
    }
}
