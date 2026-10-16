package org.printscript.linter.visitors;

import org.printscript.ast.ReadInput;

/** Collects ReadInput nodes in source traversal order, including nested blocks and reads. */
public final class ReadInputCollector extends NodeCollector<ReadInput> {
    public ReadInputCollector() {
        super(ReadInput.class);
    }
}
