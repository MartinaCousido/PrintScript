package org.printscript.parser.providers;

import java.util.List;
import org.printscript.parser.AssignmentParser;
import org.printscript.parser.DeclarationParser;
import org.printscript.parser.ExpressionBuilder;
import org.printscript.parser.Parser;
import org.printscript.parser.PrintlnParser;
import org.printscript.parser.TokenBuffer;

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
