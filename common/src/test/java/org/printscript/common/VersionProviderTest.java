package org.printscript.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VersionProviderTest {

    private static final VersionProvider<String> PROVIDER = version -> {
        if ("1.0".equals(version)) {
            return "provider-1.0";
        }
        throw VersionProvider.unknownVersion(version, "1.0");
    };

    @Test
    void aKnownVersionReturnsItsProvider() {
        assertEquals("provider-1.0", PROVIDER.providerFor("1.0"));
    }

    @Test
    void unknownVersionBuildsTheSharedFailureMessage() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> PROVIDER.providerFor("9.9"));

        assertEquals("Versión de PrintScript desconocida: 9.9. Versiones soportadas: 1.0", error.getMessage());
    }
}
