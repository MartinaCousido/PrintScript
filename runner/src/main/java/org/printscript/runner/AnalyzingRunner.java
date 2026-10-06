package org.printscript.runner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.common.Version;
import org.printscript.config.ConfigurationReader;
import org.printscript.linter.Linter;
import org.printscript.linter.LinterFactory;
import org.printscript.linter.Violation;
import org.printscript.linter.config.LinterConfiguration;

// lintea
public final class AnalyzingRunner {

    public AnalysisReport analyze(Iterator<String> lines, LinterConfiguration configuration) {
        return analyze(lines, configuration, Version.V10);
    }

    public AnalysisReport analyze(
            Iterator<String> lines, LinterConfiguration configuration, Version version) {
        List<Violation> violations = new ArrayList<>();
        List<Failure<?>> failures = new ArrayList<>();
        analyze(lines, configuration, version, violations::add, failures::add);
        return new AnalysisReport(violations, failures);
    }

    /** Streams diagnostics in statement order without retaining an entire report. */
    public boolean analyze(
            Iterator<String> lines,
            LinterConfiguration configuration,
            Version version,
            Consumer<Violation> violationReporter,
            Consumer<Failure<?>> failureReporter) {
        Iterator<Result<Statement>> statements = StatementStreams.statementStream(lines, version);
        Linter linter = LinterFactory.create(statements, configuration);
        boolean clean = true;
        while (linter.hasNext()) {
            Result<List<Violation>> result = linter.next();
            if (result instanceof Failure<List<Violation>> failure) {
                failureReporter.accept(failure);
                clean = false;
            } else {
                List<Violation> violations = ((Success<List<Violation>>) result).value();
                violations.forEach(violationReporter);
                clean = clean && violations.isEmpty();
            }
        }
        return clean;
    }

    public AnalysisReport analyze(
            Iterator<String> lines, InputStream configuration, String format) {
        return analyze(
                lines,
                LinterConfiguration.from(
                        ConfigurationReader.standard().read(configuration, format)));
    }
}
