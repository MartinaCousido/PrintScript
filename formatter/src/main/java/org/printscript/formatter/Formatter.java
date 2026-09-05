package org.printscript.formatter;

import java.util.Iterator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.common.MappedResults;
import org.printscript.common.Result;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.formatter.config.Separators;
import org.printscript.formatter.visitors.FormattingVisitor;

public final class Formatter implements Iterator<Result<String>> {

    private static final String LINE_BREAK = "\n";

    private final FormatterConfiguration configuration;
    private final FormattingVisitor visitor;
    private final Iterator<Result<String>> formatted;

    private boolean atTheTopOfTheFile = true;

    public Formatter(Iterator<Result<Statement>> statements, FormatterConfiguration configuration) {
        this.configuration = configuration;
        this.visitor = new FormattingVisitor(new Separators(configuration));
        this.formatted = new MappedResults<>(statements, this::format);
    }

    @Override
    public boolean hasNext() {
        return formatted.hasNext();
    }

    @Override
    public Result<String> next() {
        return formatted.next();
    }

    private String format(Statement statement) {
        String text = leadingBlankLines(statement) + statement.accept(visitor) + LINE_BREAK;
        atTheTopOfTheFile = false;
        return text;
    }

    private String leadingBlankLines(Statement statement) {
        if (atTheTopOfTheFile || !(statement instanceof PrintlnStatement)) {
            return "";
        }
        return LINE_BREAK.repeat(configuration.blankLinesBeforePrintln());
    }
}
