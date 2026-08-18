package org.printscript.lexer.providers;

import org.printscript.common.VersionProvider;

//la puerta de entrada al módulo: le pedís "1.0" y te da el provider.
// Si se pide una versión que no existe, falla con un mensaje claro
public final class LexerVersionProvider implements VersionProvider<LexerProvider> {

    private static final String VERSION_10 = "1.0";

    @Override
    public LexerProvider providerFor(String version) {
        if (VERSION_10.equals(version)) {
            return new LexerProvider10();
        }
        throw VersionProvider.unknownVersion(version, VERSION_10);
    }
}
