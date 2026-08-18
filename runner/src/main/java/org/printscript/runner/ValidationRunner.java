package org.printscript.runner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.parser.Parser;

public final class ValidationRunner {

    public List<Failure<?>> validate(Iterator<String> lines, String version) {
        Parser parser = StatementStreams.statementStream(lines, version);
        List<Failure<?>> failures = new ArrayList<>();
        while (parser.hasNext()) {
            Result<?> result = parser.getNext();
            if (result instanceof Failure<?> failure) {
                failures.add(failure);
            }
        }
        return failures;
    }
}
