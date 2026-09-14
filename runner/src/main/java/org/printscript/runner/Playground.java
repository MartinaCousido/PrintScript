package org.printscript.runner;

import java.util.List;
import org.printscript.common.Failure;
import org.printscript.runtime.OutputPrinter;

// es un main con un programa hardcodeado para probar a mano
public final class Playground {

    private Playground() {}

    private static final class StandardOutputPrinter implements OutputPrinter {

        @Override
        public void println(String line) {
            System.out.println(line);
        }

        @Override
        public void print(String text) {
            System.out.print(text);
        }
    }

    public static void main(String[] args) {
        List<String> program =
                List.of(
                        "let name: string = \"Joe\";",
                        "let lastName: string = \"Doe\";",
                        "println(name + \" \" + lastName);");

        List<Failure<?>> failures =
                new ExecutionRunner().run(program.iterator(), new StandardOutputPrinter());

        failures.forEach(failure -> System.out.println("ERROR: " + failure.message()));
    }
}
