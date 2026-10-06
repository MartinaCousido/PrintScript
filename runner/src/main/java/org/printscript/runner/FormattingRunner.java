package org.printscript.runner;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.common.Version;
import org.printscript.config.ConfigurationReader;
import org.printscript.formatter.Formatter;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.lexer.LexerFactory;

// formatea
public final class FormattingRunner {

    public List<Failure<?>> format(
            Iterator<String> lines, FormatterConfiguration configuration, Writer destination) {
        return format(lines, configuration, destination, Version.V10);
    }

    public List<Failure<?>> format(
            Iterator<String> lines,
            FormatterConfiguration configuration,
            Writer destination,
            Version version) {
        List<Failure<?>> failures = new ArrayList<>();
        format(lines, configuration, destination, version, failures::add);
        return failures;
    }

    /** Streams output and diagnostics, flushes, and leaves the borrowed writer open. */
    public boolean format(
            Iterator<String> lines,
            FormatterConfiguration configuration,
            Writer destination,
            Version version,
            Consumer<Failure<?>> report) {
        Formatter formatter = new Formatter(LexerFactory.create(lines, version), configuration);
        boolean clean = true;
        while (formatter.hasNext()) {
            Result<String> result = formatter.next();
            if (result instanceof Failure<String> failure) {
                report.accept(failure);
                clean = false;
            } else {
                writeText(destination, ((Success<String>) result).value());
            }
        }
        flush(destination);
        return clean;
    }

    public List<Failure<?>> format(
            Iterator<String> lines, InputStream configuration, String format, Writer destination) {
        return format(
                lines,
                FormatterConfiguration.from(
                        ConfigurationReader.standard().read(configuration, format)),
                destination);
    }

    private static void writeText(Writer destination, String text) {
        try {
            destination.write(text);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void flush(Writer destination) {
        try {
            destination.flush();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
