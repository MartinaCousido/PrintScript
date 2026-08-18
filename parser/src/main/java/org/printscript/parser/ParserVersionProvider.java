package org.printscript.parser;

import org.printscript.common.VersionProvider;

public final class ParserVersionProvider implements VersionProvider<ParserProvider> {

    private static final String VERSION_10 = "1.0";

    @Override
    public ParserProvider providerFor(String version) {
        if (VERSION_10.equals(version)) {
            return new ParserProvider10();
        }
        throw VersionProvider.unknownVersion(version, VERSION_10);
    }
}
