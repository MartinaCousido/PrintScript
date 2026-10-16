package org.printscript.cli;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import org.printscript.common.Failure;
import org.printscript.common.Version;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

// delega en SourceFileProcessor la lectura del archivo, el progreso y el reporte de
// fallas, aportando solo la operación concreta (execution/validation); cero lógica
// de lenguaje ni de filesystem acá
abstract class AbstractSourceFileCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "ARCHIVO", description = "Archivo .ps a procesar")
    private Path sourceFile;

    @ParentCommand private RootCommand root;

    protected abstract boolean execute(
            Iterator<String> lines, Version version, Consumer<Failure<?>> report);

    @Override
    public final Integer call() {
        return SourceFileProcessor.processStreaming(
                        sourceFile,
                        (lines, report) -> execute(lines, root.language(), report),
                        System.err)
                .code();
    }
}
