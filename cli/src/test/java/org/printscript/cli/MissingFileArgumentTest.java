package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MissingFileArgumentTest {

    @Test
    void nonexistentFileFailsWithUsageError() {
        CliTestSupport.Result result = CliTestSupport.run("execution", "/no/existe/archivo.ps");

        assertEquals(2, result.exitCode());
    }

    @Test
    void missingPositionalArgumentFailsWithUsageError() {
        CliTestSupport.Result result = CliTestSupport.run("execution");

        assertEquals(2, result.exitCode());
    }

    @Test
    void noOperationAtAllFailsWithUsageError() {
        CliTestSupport.Result result = CliTestSupport.run();

        assertEquals(2, result.exitCode());
    }
}
