package org.printscript.parser;

import java.util.List;

public final class ParserFactory {

    private ParserFactory() {}

    public static Parser create(TokenBuffer tokens) {
        ExpressionBuilder expressionBuilder = new ExpressionBuilder();
        return new Parser(
                tokens,
                List.of(
                        new DeclarationParser(expressionBuilder),
                        new AssignmentParser(expressionBuilder),
                        new PrintlnParser(expressionBuilder)),
                new SemicolonDelimiter());
    }
}
