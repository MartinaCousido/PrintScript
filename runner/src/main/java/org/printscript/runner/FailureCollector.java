package org.printscript.runner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.printscript.common.Failure;
import org.printscript.common.Result;

final class FailureCollector {

    private FailureCollector() {}

    static List<Failure<?>> collect(Iterator<? extends Result<?>> results) {
        List<Failure<?>> failures = new ArrayList<>();
        while (results.hasNext()) {
            if (results.next() instanceof Failure<?> failure) {
                failures.add(failure);
            }
        }
        return failures;
    }
}
