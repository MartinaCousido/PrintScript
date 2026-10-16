package org.printscript.runner;

import java.util.Iterator;
import java.util.function.Consumer;
import org.printscript.common.Failure;
import org.printscript.common.Result;

final class FailureCollector {

    private FailureCollector() {}

    static boolean consume(Iterator<? extends Result<?>> results, Consumer<Failure<?>> report) {
        boolean clean = true;
        while (results.hasNext()) {
            if (results.next() instanceof Failure<?> failure) {
                report.accept(failure);
                clean = false;
            }
        }
        return clean;
    }
}
