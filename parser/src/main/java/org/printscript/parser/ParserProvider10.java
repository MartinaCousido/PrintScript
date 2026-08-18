package org.printscript.parser;

import java.util.List;

public final class ParserProvider10 implements ParserProvider {

    @Override
    public Parser provide(TokenBuffer tokens) {
        ExpressionBuilder expressionBuilder = new ExpressionBuilder();
        return new Parser(
            tokens,
            List.of(
                new DeclarationParser(expressionBuilder),
                new AssignmentParser(expressionBuilder),
                new PrintlnParser(expressionBuilder)));
    }
}
