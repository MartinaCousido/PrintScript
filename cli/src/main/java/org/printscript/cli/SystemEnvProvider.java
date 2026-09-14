package org.printscript.cli;

import java.util.Optional;
import org.printscript.runtime.EnvProvider;

// llama a System.getenv
final class SystemEnvProvider implements EnvProvider {

    @Override
    public Optional<String> read(String name) {
        return Optional.ofNullable(System.getenv(name));
    }
}
