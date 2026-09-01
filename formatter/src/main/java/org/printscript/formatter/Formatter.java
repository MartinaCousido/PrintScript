package org.printscript.formatter;

import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.common.MappedResults;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.formatter.config.Separators;
import org.printscript.formatter.visitors.FormattingVisitor;

public final class Formatter implements PrintScriptIterator<Result<String>> {

    private static final String LINE_BREAK = "\n";

    private final FormatterConfiguration configuration;
    private final FormattingVisitor visitor;
    private final PrintScriptIterator<Result<String>> formatted;

    private boolean atTheTopOfTheFile = true;

    public Formatter(
            PrintScriptIterator<Result<Statement>> statements,
            FormatterConfiguration configuration) {
        this.configuration = configuration;
        this.visitor = new FormattingVisitor(new Separators(configuration));
        this.formatted = new MappedResults<>(statements, this::format);
    }

    @Override
    public boolean hasNext() {
        return formatted.hasNext();
    }

    @Override
    public Result<String> getNext() {
        return formatted.getNext();
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
