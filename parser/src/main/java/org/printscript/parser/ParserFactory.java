package org.printscript.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
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
        ExpressionBuilder expressionBuilder = new ExpressionBuilder(reads(version));
        List<StatementParser> common =
                List.of(
                        new DeclarationParser(expressionBuilder, declarators(version)),
                        new AssignmentParser(expressionBuilder),
                        new PrintlnParser(expressionBuilder));
        return switch (version) {
            case V10 -> common;
            // el if se pasa la lista como Supplier porque se contiene a sí mismo: es uno
            // de los parsers que puede aparecer adentro de su propio cuerpo. La lista se
            // arma una sola vez y el Supplier devuelve siempre esa misma instancia; si en
            // cambio recomputara statementParsers(V11), cada sentencia de cada bloque
            // reconstruiría todo el grafo de parsers.
            case V11 -> {
                AtomicReference<List<StatementParser>> self = new AtomicReference<>();
                List<StatementParser> all =
                        concat(common, new IfParser(expressionBuilder, self::get));
                self.set(all);
                yield all;
            }
        };
    }

    private static List<StatementParser> concat(
            List<StatementParser> parsers, StatementParser one) {
        List<StatementParser> all = new ArrayList<>(parsers);
        all.add(one);
        return List.copyOf(all);
    }

    // agregar una función de lectura es agregar una entrada acá
    private static Map<String, ReadFactory> reads(Version version) {
        return switch (version) {
            case V10 -> Map.of();
            case V11 -> Map.of("readInput", ReadInput::new, "readEnv", ReadEnv::new);
        };
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
