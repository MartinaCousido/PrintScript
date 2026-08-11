package org.printscript.ast;

import java.util.Optional;
import org.printscript.common.Range;

public record VariableDeclaration(String name, TypeName type, Optional<Expression> initializer, Range range) implements Statement {
}
