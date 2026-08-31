package org.printscript.cli;

import java.util.Iterator;
import java.util.List;
import org.printscript.common.Failure;
import org.printscript.runner.ExecutionRunner;
import picocli.CommandLine.Command;

@Command(
        name = "execution",
        aliases = {"exec"},
        description = "Ejecuta un archivo PrintScript, imprimiendo su salida por stdout")
final class ExecutionCommand extends AbstractSourceFileCommand {

    @Override
    protected List<Failure<?>> execute(Iterator<String> lines) {
        return new ExecutionRunner().run(lines, new ConsoleOutputPrinter());
    }
}
