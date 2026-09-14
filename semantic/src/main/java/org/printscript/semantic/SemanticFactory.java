package org.printscript.semantic;

import java.util.Iterator;
import java.util.Set;
import org.printscript.ast.Statement;
import org.printscript.common.Result;
import org.printscript.common.Version;

// acá vive lo que semantic sabe de cada versión

public final class SemanticFactory {

    private SemanticFactory() {}

    public static SemanticAnalyzer create(Iterator<Result<Statement>> statements, Version version) {
        return new SemanticAnalyzer(statements, new StatementChecker(supportedTypes(version)));
    }

    private static Set<Type> supportedTypes(Version version) {
        return switch (version) {
            case V10 -> Set.of(Type.NUMBER, Type.STRING);
            case V11 -> Set.of(Type.NUMBER, Type.STRING, Type.BOOLEAN);
        };
    }
}
