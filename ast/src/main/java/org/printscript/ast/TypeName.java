package org.printscript.ast;

import org.printscript.common.Range;
//el 'tipo' tal como se escribió en el código
public record TypeName(String name, Range range) {
}
