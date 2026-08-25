package org.printscript.runner;

import java.util.Iterator;
import java.util.List;
import org.printscript.common.PrintScriptIterators;
import org.printscript.lexer.Lexer;
import org.printscript.lexer.providers.LexerVersionProvider;
import org.printscript.token.Token;

public final class Playground {

    private Playground() {
    }

    public static void main(String[] args) {
        List<String> program =
                List.of(
                        "let name: string = \"Joe\";",
                        "println(name + \" \" + lastName);");

        Iterator<String> lines = program.iterator();
        Lexer lexer = new LexerVersionProvider().providerFor("1.0").provide(lines);

        PrintScriptIterators.stream(lexer)
                .forEach(
                        token ->
                                System.out.println(
                                        token.type() + "  \"" + token.value() + "\"  " + token.range()));
    }
}
