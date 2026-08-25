package org.printscript.runner;

import java.util.List;
import org.printscript.common.Failure;

public final class Playground {

    private Playground() {}

    public static void main(String[] args) {
        List<String> program =
                List.of(
                        "let name: string = \"Joe\";",
                        "let lastName: string = \"Doe\";",
                        "println(name + \" \" + lastName);");

        List<Failure<?>> failures =
                new ExecutionRunner().run(program.iterator(), System.out::println);

        failures.forEach(failure -> System.out.println("ERROR: " + failure.message()));
    }
}
