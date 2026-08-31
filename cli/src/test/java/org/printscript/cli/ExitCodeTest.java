package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExitCodeTest {

    @Test
    void codesMatchDocumentedValues() {
        assertEquals(0, ExitCode.SUCCESS.code());
        assertEquals(1, ExitCode.SOURCE_ERRORS.code());
        assertEquals(2, ExitCode.USAGE_ERROR.code());
        assertEquals(3, ExitCode.UNSUPPORTED_OPERATION.code());
    }
}
