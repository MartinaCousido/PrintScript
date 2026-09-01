package org.printscript.runner;

import java.util.List;
import org.printscript.common.Failure;
import org.printscript.linter.Violation;

public record AnalysisReport(List<Violation> violations, List<Failure<?>> failures) {

    public AnalysisReport {
        violations = List.copyOf(violations);
        failures = List.copyOf(failures);
    }
}
