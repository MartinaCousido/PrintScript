package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Block;
import org.printscript.ast.IfStatement;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.common.Version;

class IfParserTest {

    private static Result<Statement> parse(String source) {
        return ParserFactory.create(Lexemes.bufferOf(source), Version.V11).getNext();
    }

    private static IfStatement conditional(String source) {
        return (IfStatement) assertInstanceOf(Success.class, parse(source)).value();
    }

    @Test
    void parsesAnIfWithoutElse() {
        IfStatement conditional = conditional("if ( flag ) { println ( 1 ) ; }");

        assertEquals(1, conditional.thenBlock().statements().size());
        assertInstanceOf(PrintlnStatement.class, conditional.thenBlock().statements().getFirst());
        assertTrue(conditional.elseBranch().isEmpty());
    }

    @Test
    void parsesAnIfWithElse() {
        IfStatement conditional =
                conditional("if ( flag ) { println ( 1 ) ; } else { println ( 2 ) ; }");

        Block elseBlock = assertInstanceOf(Block.class, conditional.elseBranch().orElseThrow());
        assertEquals(1, elseBlock.statements().size());
    }

    @Test
    void anEmptyBlockIsValid() {
        assertEquals(0, conditional("if ( flag ) { }").thenBlock().statements().size());
    }

    @Test
    void parsesThreeLevelsOfNesting() {
        // el caso que rompe cuando el bloque se delimita buscando la primera '}'
        IfStatement outer = conditional("if ( a ) { if ( b ) { if ( c ) { println ( 1 ) ; } } }");

        IfStatement middle =
                assertInstanceOf(IfStatement.class, outer.thenBlock().statements().getFirst());
        IfStatement inner =
                assertInstanceOf(IfStatement.class, middle.thenBlock().statements().getFirst());
        assertInstanceOf(PrintlnStatement.class, inner.thenBlock().statements().getFirst());
    }

    @Test
    void theOuterBlockKeepsEverythingThatComesAfterANestedIf() {
        IfStatement outer =
                conditional("if ( a ) { if ( b ) { println ( 1 ) ; } println ( 2 ) ; }");

        assertEquals(2, outer.thenBlock().statements().size());
        assertInstanceOf(IfStatement.class, outer.thenBlock().statements().get(0));
        assertInstanceOf(PrintlnStatement.class, outer.thenBlock().statements().get(1));
    }

    @Test
    void anElseIfIsAnIfInTheElseBranchWithNoBlockAroundIt() {
        IfStatement conditional =
                conditional("if ( a ) { println ( 1 ) ; } else if ( b ) { println ( 2 ) ; }");

        assertInstanceOf(IfStatement.class, conditional.elseBranch().orElseThrow());
    }

    @Test
    void anElseWithABlockContainingAnIfIsNotTheSameTree() {
        // la diferencia es la razón por la que Block es un nodo: el formatter imprime
        // uno en la misma línea y el otro anidado
        IfStatement conditional =
                conditional("if ( a ) { println ( 1 ) ; } else { if ( b ) { println ( 2 ) ; } }");

        Block elseBlock = assertInstanceOf(Block.class, conditional.elseBranch().orElseThrow());
        assertInstanceOf(IfStatement.class, elseBlock.statements().getFirst());
    }

    @Test
    void parsesAChainOfElseIfEndingInAnElse() {
        IfStatement first =
                conditional(
                        "if ( a ) { println ( 1 ) ; }"
                                + " else if ( b ) { println ( 2 ) ; }"
                                + " else { println ( 3 ) ; }");

        IfStatement second = assertInstanceOf(IfStatement.class, first.elseBranch().orElseThrow());
        assertInstanceOf(Block.class, second.elseBranch().orElseThrow());
    }

    @Test
    void theConditionCanBeMoreThanOneToken() {
        // el índice fijo del cuerpo es la otra forma de romper esto
        IfStatement conditional = conditional("if ( a + b ) { println ( 1 ) ; }");

        assertInstanceOf(BinaryExpression.class, conditional.condition());
        assertEquals(1, conditional.thenBlock().statements().size());
    }

    @Test
    void aSyntaxErrorInsideABlockTravelsAsDataWithItsOwnRange() {
        Failure<?> failure = assertInstanceOf(Failure.class, parse("if ( a ) { println ( 1 ) }"));

        assertEquals("Expected ';' at the end of the println statement", failure.message());
    }

    @Test
    void anUnrecognizedStatementInsideABlockIsReportedAtItsFirstToken() {
        Failure<?> failure = assertInstanceOf(Failure.class, parse("if ( a ) { 42 ; }"));

        assertEquals("Unrecognized statement", failure.message());
        assertEquals(12, failure.range().start().column());
    }

    @Test
    void aBlockThatNeverClosesIsReported() {
        assertInstanceOf(Failure.class, parse("if ( a ) { println ( 1 ) ;"));
    }

    @Test
    void version10DoesNotKnowTheIfStatement() {
        Result<Statement> result =
                ParserFactory.create(Lexemes.bufferOf("if ( a ) { println ( 1 ) ; }"), Version.V10)
                        .getNext();

        assertFalse(result instanceof Success<Statement>);
    }
}
