package org.printscript.parser;

import java.util.Optional;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.Statement;
import org.printscript.ast.TypeName;
import org.printscript.common.Range;

// arma el nodo de una declaración a partir de sus partes
// agregar un declarador nuevo a una versión es agregar una entrada a la tabla del
// ParserFactory: el DeclarationParser no gana ninguna rama
@FunctionalInterface
public interface DeclarationFactory {

    Statement build(Identifier name, TypeName type, Optional<Expression> initializer, Range range);
}
