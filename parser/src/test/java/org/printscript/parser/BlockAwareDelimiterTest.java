package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.token.Token;

class BlockAwareDelimiterTest {

    private final BlockAwareDelimiter delimiter = new BlockAwareDelimiter();

    // corre el criterio sobre todos los tokens y devuelve dónde cortó cada sentencia
    private List<String> statements(String source) {
        List<Token> tokens = Lexemes.tokensOf(source);
        List<String> cut = new ArrayList<>();
        List<Token> accumulated = new ArrayList<>();
        for (int index = 0; index < tokens.size(); index = index + 1) {
            Token current = tokens.get(index);
            int following = index + 1;
            boolean ends =
                    delimiter.endsStatement(
                            List.copyOf(accumulated),
                            current,
                            () ->
                                    following < tokens.size()
                                            ? Optional.of(tokens.get(following))
                                            : Optional.empty());
            accumulated.add(current);
            if (ends) {
                cut.add(describe(accumulated));
                accumulated.clear();
            }
        }
        if (!accumulated.isEmpty()) {
            cut.add("SIN CERRAR: " + describe(accumulated));
        }
        return cut;
    }

    private static String describe(List<Token> tokens) {
        return String.join(" ", tokens.stream().map(Token::value).toList());
    }

    @Test
    void aStatementWithoutBracesEndsAtItsSemicolon() {
        assertEquals(
                List.of("let x : number = 1 ;", "println ( x ) ;"),
                statements("let x : number = 1 ; println ( x ) ;"));
    }

    @Test
    void theSemicolonsInsideABlockDoNotEndTheStatement() {
        assertEquals(
                List.of("if ( flag ) { println ( 1 ) ; }"),
                statements("if ( flag ) { println ( 1 ) ; }"));
    }

    @Test
    void aNestedIfEndsAtTheBraceThatClosesTheOuterBlock() {
        // es el caso que se rompe cuando se busca "la primera llave que cierra"
        assertEquals(
                List.of("if ( a ) { if ( b ) { x = 1 ; } }"),
                statements("if ( a ) { if ( b ) { x = 1 ; } }"));
    }

    @Test
    void threeLevelsOfNestingStillEndAtTheOutermostBrace() {
        String source = "if ( a ) { if ( b ) { if ( c ) { x = 1 ; } } }";

        assertEquals(List.of(source), statements(source));
    }

    @Test
    void anElseContinuesTheSameStatement() {
        assertEquals(
                List.of("if ( a ) { x = 1 ; } else { x = 2 ; }"),
                statements("if ( a ) { x = 1 ; } else { x = 2 ; }"));
    }

    @Test
    void aChainOfElseIfArrivesAsASingleStatement() {
        String chain = "if ( a ) { x = 1 ; } else if ( b ) { x = 2 ; } else { x = 3 ; }";

        assertEquals(List.of(chain), statements(chain));
    }

    @Test
    void theStatementAfterAnIfIsCutOnItsOwn() {
        assertEquals(
                List.of("if ( a ) { x = 1 ; }", "println ( x ) ;"),
                statements("if ( a ) { x = 1 ; } println ( x ) ;"));
    }

    @Test
    void aStrayClosingBraceIsCutOnItsOwnSoTheParserCanReportIt() {
        assertEquals(List.of("}", "println ( x ) ;"), statements("} println ( x ) ;"));
    }

    @Test
    void anEmptyBlockIsValid() {
        assertEquals(List.of("if ( a ) { }"), statements("if ( a ) { }"));
    }

    @Test
    void theMessageSaysWhatIsMissingDependingOnWhereItStopped() {
        assertEquals(
                "Expected '}' before the end of the stream",
                delimiter.unterminatedMessage(Lexemes.tokensOf("if ( a ) { x = 1 ;")));
        assertEquals(
                "Expected ';' before the end of the stream",
                delimiter.unterminatedMessage(Lexemes.tokensOf("let x : number = 1")));
    }
}
