package org.printscript.parser;

import org.printscript.ast.Expression;
import org.printscript.common.Range;

// arma el nodo de una función de lectura a partir de su argumento
// agregar una función invocable a una versión es agregar una entrada a la tabla del
// ParserFactory: el ExpressionBuilder no gana ninguna rama
@FunctionalInterface
public interface ReadFactory {

    Expression build(Expression argument, Range range);
}
