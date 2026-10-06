package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Version;
import org.printscript.runtime.OutputPrinter;

class DeepProgramTest {
    private static final int STRESS_DEPTH = 10_000;

    @Test
    void nestedParenthesesExecuteWithoutOverflowingTheJvmStack() {
        assertProgram(
                "println(" + "(".repeat(STRESS_DEPTH) + "1" + ")".repeat(STRESS_DEPTH) + ");", "1");
    }

    @Test
    void longLeftAssociativeExpressionsAreTypedAndEvaluatedIteratively() {
        assertProgram(
                "println(" + "1 + ".repeat(STRESS_DEPTH) + "1);",
                Integer.toString(STRESS_DEPTH + 1));
    }

    @Test
    void deeplyNestedConditionalsParseCheckAndExecuteWithoutStackOverflow() {
        assertProgram(
                "if (true) {".repeat(STRESS_DEPTH) + "println(1);" + "}".repeat(STRESS_DEPTH), "1");
    }

    @Test
    void longElseIfChainsDoNotUseTheJvmCallStack() {
        assertProgram(
                "if (false) {} else ".repeat(STRESS_DEPTH) + "if (true) { println(1); }", "1");
    }

    private static void assertProgram(String source, String expected) {
        List<String> output = new ArrayList<>();
        OutputPrinter printer =
                new OutputPrinter() {
                    @Override
                    public void println(String line) {
                        output.add(line);
                    }

                    @Override
                    public void print(String text) {
                        output.add(text);
                    }
                };
        assertEquals(
                List.of(),
                new ExecutionRunner().run(List.of(source).iterator(), printer, Version.V11));
        assertEquals(List.of(expected), output);
    }
}
