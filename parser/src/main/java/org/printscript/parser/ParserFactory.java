package org.printscript.parser;

import java.util.List;
import org.printscript.common.Version;

// acá vive todo lo que el parser sabe de cada versión del lenguaje: qué sentencias
// existen y dónde termina una sentencia. El motor recibe las dos cosas como datos.
public final class ParserFactory {

    private ParserFactory() {}

    public static Parser create(TokenBuffer tokens, Version version) {
        return new Parser(tokens, statementParsers(version), delimiter(version));
    }

    // agregar una sentencia a una versión es agregar un parser a esta lista
    private static List<StatementParser> statementParsers(Version version) {
        ExpressionBuilder expressionBuilder = new ExpressionBuilder();
        List<StatementParser> common =
                List.of(
                        new DeclarationParser(expressionBuilder),
                        new AssignmentParser(expressionBuilder),
                        new PrintlnParser(expressionBuilder));
        return switch (version) {
            // el parser del if entra en 1.1 junto con los nodos Block e IfStatement
            case V10, V11 -> common;
        };
    }

    private static StatementDelimiter delimiter(Version version) {
        return switch (version) {
            // 1.1 pasa a cortar también en la llave que cierra un bloque
            case V10, V11 -> new SemicolonDelimiter();
        };
    }
}
