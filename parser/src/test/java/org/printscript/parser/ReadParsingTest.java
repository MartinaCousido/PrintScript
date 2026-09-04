package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.common.Version;

// las lecturas externas entran por una tabla del ParserFactory: el ExpressionBuilder no
// sabe cuáles existen, solo que una keyword de la tabla se parsea como llamado
class ReadParsingTest {

    private static Result<Statement> parse(Version version, String source) {
        return ParserFactory.create(Lexemes.bufferOf(source), version).getNext();
    }

    private static Object initializerOf(String source) {
        VariableDeclaration declaration =
                (VariableDeclaration)
                        assertInstanceOf(Success.class, parse(Version.V11, source)).value();
        return declaration.initializer().orElseThrow();
    }

    @Test
    void readInputBuildsItsNodeWithItsArgument() {
        ReadInput read =
                assertInstanceOf(
                        ReadInput.class, initializerOf("let n : string = readInput ( 'p' ) ;"));

        assertInstanceOf(StringLiteral.class, read.prompt());
    }

    @Test
    void readEnvBuildsItsNodeWithItsArgument() {
        ReadEnv read =
                assertInstanceOf(
                        ReadEnv.class, initializerOf("let n : string = readEnv ( 'P' ) ;"));

        assertInstanceOf(StringLiteral.class, read.name());
    }

    @Test
    void aReadCanTakePartInAnExpression() {
        assertInstanceOf(
                BinaryExpression.class,
                initializerOf("let n : string = readInput ( 'p' ) + 'x' ;"));
    }

    @Test
    void theArgumentCanBeAnExpressionOnItsOwn() {
        ReadInput read =
                assertInstanceOf(
                        ReadInput.class,
                        initializerOf("let n : string = readInput ( 'a' + 'b' ) ;"));

        assertInstanceOf(BinaryExpression.class, read.prompt());
    }

    @Test
    void aParenthesizedReadKeepsItsArgumentAndTakesTheRangeOfTheParentheses() {
        ReadInput read =
                assertInstanceOf(
                        ReadInput.class, initializerOf("let n : string = ( readInput ( 'p' ) ) ;"));

        assertInstanceOf(StringLiteral.class, read.prompt());
    }

    @Test
    void aParenthesizedReadEnvKeepsItsArgument() {
        ReadEnv read =
                assertInstanceOf(
                        ReadEnv.class, initializerOf("let n : string = ( readEnv ( 'P' ) ) ;"));

        assertInstanceOf(StringLiteral.class, read.name());
    }

    @Test
    void aReadWithoutParenthesisIsReported() {
        Failure<?> failure =
                assertInstanceOf(Failure.class, parse(Version.V11, "let n : string = readInput ;"));

        assertEquals("Expected '(' after 'readInput'", failure.message());
    }

    @Test
    void aReadThatNeverClosesIsReported() {
        Failure<?> failure =
                assertInstanceOf(
                        Failure.class, parse(Version.V11, "let n : string = readInput ( 'p' ;"));

        assertEquals("Expected ')' after the argument of 'readInput'", failure.message());
    }

    @Test
    void inVersion10AKeywordInOperandPositionIsStillAnError() {
        Failure<?> failure =
                assertInstanceOf(
                        Failure.class, parse(Version.V10, "let n : string = readInput ( 'p' ) ;"));

        assertEquals("Unexpected token 'readInput' in operand position", failure.message());
    }
}
