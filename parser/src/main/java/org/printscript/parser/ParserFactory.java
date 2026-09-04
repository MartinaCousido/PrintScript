package org.printscript.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.VariableDeclaration;
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
                        new DeclarationParser(expressionBuilder, declarators(version)),
                        new AssignmentParser(expressionBuilder),
                        new PrintlnParser(expressionBuilder));
        return switch (version) {
            case V10 -> common;
            // el if se pasa la lista como Supplier porque se contiene a sí mismo: es uno
            // de los parsers que puede aparecer adentro de su propio cuerpo
            case V11 ->
                    concat(
                            common,
                            new IfParser(expressionBuilder, () -> statementParsers(Version.V11)));
        };
    }

    private static List<StatementParser> concat(
            List<StatementParser> parsers, StatementParser one) {
        List<StatementParser> all = new ArrayList<>(parsers);
        all.add(one);
        return List.copyOf(all);
    }

    // agregar un declarador es agregar una entrada acá: el DeclarationParser no cambia
    private static Map<String, DeclarationFactory> declarators(Version version) {
        return switch (version) {
            case V10 -> Map.of("let", VariableDeclaration::new);
            case V11 ->
                    Map.of(
                            "let", VariableDeclaration::new,
                            "const", ConstDeclaration::new);
        };
    }

    private static StatementDelimiter delimiter(Version version) {
        return switch (version) {
            case V10 -> new SemicolonDelimiter();
            case V11 -> new BlockAwareDelimiter();
        };
    }
}
