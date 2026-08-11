package org.printscript.ast;

import org.printscript.common.Range;

public sealed interface Statement permits VariableDeclaration, AssignmentStatement, PrintlnStatement {

    Range range();
}
