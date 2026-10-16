package org.printscript.cli;

import java.util.Iterator;
import java.util.function.Consumer;
import org.printscript.common.Failure;
import org.printscript.common.Version;
import org.printscript.runner.ValidationRunner;
import picocli.CommandLine.Command;

@Command(
        name = "validation",
        aliases = {"validate"},
        description = "Valida la sintaxis y semántica de un archivo PrintScript sin ejecutarlo")
final class ValidationCommand extends AbstractSourceFileCommand {

    @Override
    protected boolean execute(
            Iterator<String> lines, Version version, Consumer<Failure<?>> report) {
        return new ValidationRunner().validate(lines, version, report);
    }
}
