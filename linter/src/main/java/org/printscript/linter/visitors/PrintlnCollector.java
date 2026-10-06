package org.printscript.linter.visitors;

import org.printscript.ast.PrintlnStatement;

/** Collects PrintlnStatement nodes in source traversal order, including nested blocks and reads. */
public final class PrintlnCollector extends NodeCollector<PrintlnStatement> {
    public PrintlnCollector() {
        super(PrintlnStatement.class);
    }
}
