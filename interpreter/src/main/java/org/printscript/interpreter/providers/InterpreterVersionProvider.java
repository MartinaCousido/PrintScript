package org.printscript.interpreter.providers;

import org.printscript.common.VersionProvider;

public final class InterpreterVersionProvider implements VersionProvider<InterpreterProvider> {

    private static final String VERSION_10 = "1.0";

    @Override
    public InterpreterProvider providerFor(String version) {
        if (VERSION_10.equals(version)) {
            return new InterpreterProvider10();
        }
        throw VersionProvider.unknownVersion(version, VERSION_10);
    }
}
