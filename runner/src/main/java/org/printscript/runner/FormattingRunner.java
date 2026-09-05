package org.printscript.runner;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.config.ConfigurationReader;
import org.printscript.config.JsonConfigurationMapper;
import org.printscript.config.YamlConfigurationMapper;
import org.printscript.formatter.Formatter;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.parser.Parser;

public final class FormattingRunner {

    public List<Failure<?>> format(
            Iterator<String> lines, FormatterConfiguration configuration, Writer destination) {
        Parser parser = StatementStreams.statementStream(lines);
        Formatter formatter = new Formatter(parser, configuration);
        List<Failure<?>> failures = new ArrayList<>();
        while (formatter.hasNext()) {
            write(formatter.next(), destination, failures);
        }
        flush(destination);
        return failures;
    }

    public List<Failure<?>> format(
            Iterator<String> lines, InputStream configuration, String format, Writer destination) {
        ConfigurationReader reader =
                new ConfigurationReader(
                        List.of(new JsonConfigurationMapper(), new YamlConfigurationMapper()));
        return format(
                lines,
                FormatterConfiguration.from(reader.read(configuration, format)),
                destination);
    }

    private static void write(
            Result<String> result, Writer destination, List<Failure<?>> failures) {
        if (result instanceof Failure<String> failure) {
            failures.add(failure);
            return;
        }
        try {
            destination.write(((Success<String>) result).value());
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
