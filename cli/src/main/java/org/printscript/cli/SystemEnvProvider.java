package org.printscript.cli;

import java.util.Optional;
import org.printscript.runtime.EnvProvider;

// el único lugar del sistema autorizado a llamar a System.getenv
// cli es el único módulo que toca el mundo exterior (docs/arquitectura.md, sección 5)
final class SystemEnvProvider implements EnvProvider {

    @Override
    public Optional<String> read(String name) {
        return Optional.ofNullable(System.getenv(name));
    }
}
