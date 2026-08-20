package org.printscript.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VersionProviderTest {

    private static final String VERSION_10 = "1.0";

    private static final VersionProvider<String> PROVIDER =
            version -> {
                if (VERSION_10.equals(version)) {
                    return "provider-1.0";
                }
                throw VersionProvider.unknownVersion(version, VERSION_10);
            };

    @Test
    void aKnownVersionReturnsItsProvider() {
        assertEquals("provider-1.0", PROVIDER.providerFor(VERSION_10));
    }

    @Test
    void unknownVersionBuildsTheSharedFailureMessage() {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> PROVIDER.providerFor("9.9"));

        assertEquals(
                "Versión de PrintScript desconocida: 9.9. Versiones soportadas: 1.0",
                error.getMessage());
    }
}
