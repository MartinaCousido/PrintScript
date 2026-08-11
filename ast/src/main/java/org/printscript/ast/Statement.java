package org.printscript.ast;

import org.printscript.common.Range;
//algo que hace algo
public sealed interface Statement permits VariableDeclaration, AssignmentStatement, PrintlnStatement {

    Range range();

    <T> T accept(ASTVisitor<T> visitor);
}
