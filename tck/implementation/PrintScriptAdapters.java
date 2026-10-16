package implementation;

import interpreter.InputProvider;
import interpreter.PrintEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;

import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Version;
import org.printscript.runtime.OutputPrinter;

// Las piezas que comparten los tres adaptadores.
//
// Cada uno traduce entre dos vocabularios: el del TCK (InputStream, String de version,
// handlers) y el de la implementación (Iterator<String>, Version, puertos). Nada de esto
// vive en el repo de PrintScript a propósito: es la forma del TCK, no la del lenguaje.
final class PrintScriptAdapters {

    private PrintScriptAdapters() {}

    // el fuente llega como stream y se recorre de a una línea: el TCK corre los tests con
    // heap pequeño (5 MB con nuestra adaptación de Java 25); no se materializa el fuente
    static Iterator<String> lines(InputStream source) {
        return new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))
                .lines()
                .iterator();
    }

    static Version versionOf(String version) {
        return switch (version) {
            case "1.0" -> Version.V10;
            case "1.1" -> Version.V11;
            default ->
                    throw new IllegalArgumentException("Versión no soportada por la implementación: " + version);
        };
    }

    static String describe(String message, Range range) {
        Position start = range.start();
        return message + " (línea " + start.line() + ", columna " + start.column() + ")";
    }

    // Traduce la salida del programa al emisor del TCK.
    //
    // println y print van los dos a print(...) porque el TCK cuenta un mensaje por cada
    // cosa que el programa muestra, y el prompt de readInput es una de ellas: su caso
    // espera ["Name:", "Hello world!"] para un readInput seguido de un println.
    static final class EmittingPrinter implements OutputPrinter {

        private final PrintEmitter emitter;
        // el último prompt, para dárselo al proveedor de entrada del TCK, que lo recibe
        // como nombre. La implementación no se lo pasa a su puerto, así que se recuerda acá.
        private String lastPrompt = "";

        EmittingPrinter(PrintEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void println(String line) {
            emitter.print(line);
        }

        @Override
        public void print(String text) {
            lastPrompt = text;
            emitter.print(text);
        }

        String lastPrompt() {
            return lastPrompt;
        }
    }

    static org.printscript.runtime.InputProvider inputPort(
            InputProvider provider, EmittingPrinter printer) {
        return () -> Optional.ofNullable(provider.input(printer.lastPrompt()));
    }

    static org.printscript.runtime.EnvProvider envPort() {
        return name -> Optional.ofNullable(System.getenv(name));
    }
}
