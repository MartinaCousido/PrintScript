package org.printscript.runner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.common.Version;
import org.printscript.config.ConfigurationReader;
import org.printscript.config.JsonConfigurationMapper;
import org.printscript.config.YamlConfigurationMapper;
import org.printscript.linter.Linter;
import org.printscript.linter.LinterFactory;
import org.printscript.linter.Violation;
import org.printscript.linter.config.LinterConfiguration;

public final class AnalyzingRunner {

    // la consigna define la versión como argumento opcional, con 1.0 por defecto
    public AnalysisReport analyze(Iterator<String> lines, LinterConfiguration configuration) {
        return analyze(lines, configuration, Version.V10);
    }

    public AnalysisReport analyze(
            Iterator<String> lines, LinterConfiguration configuration, Version version) {
        // sin semantic a propósito: se tiene que poder trabajar sobre código con
        // errores de tipos (ver StatementStreams)
        Iterator<Result<Statement>> statements = StatementStreams.statementStream(lines, version);
        Linter linter = LinterFactory.create(statements, configuration);
        List<Violation> violations = new ArrayList<>();
        List<Failure<?>> failures = new ArrayList<>();
        while (linter.hasNext()) {
            collect(linter.next(), violations, failures);
        }
        return new AnalysisReport(violations, failures);
    }

    public AnalysisReport analyze(
            Iterator<String> lines, InputStream configuration, String format) {
        ConfigurationReader reader =
                new ConfigurationReader(
                        List.of(new JsonConfigurationMapper(), new YamlConfigurationMapper()));
        return analyze(lines, LinterConfiguration.from(reader.read(configuration, format)));
    }

    private static void collect(
            Result<List<Violation>> result, List<Violation> violations, List<Failure<?>> failures) {
        if (result instanceof Failure<List<Violation>> failure) {
            failures.add(failure);
        } else if (result instanceof Success<List<Violation>> success) {
            violations.addAll(success.value());
        }
    }
}
