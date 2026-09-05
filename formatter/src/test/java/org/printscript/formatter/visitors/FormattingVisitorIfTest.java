package org.printscript.formatter.visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.Block;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.IfStatement;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.formatter.config.Separators;

class FormattingVisitorIfTest {

    private static final Range RANGE = new Range(new Position(1, 1), new Position(1, 4));

    private static FormattingVisitor visitor(int indent, boolean braceOnSameLine) {
        return new FormattingVisitor(
                new Separators(
                        new FormatterConfiguration(false, true, true, 0, indent, braceOnSameLine)));
    }

    private static Statement println(int value) {
        return new PrintlnStatement(new NumberLiteral(value, RANGE), RANGE);
    }

    private static Block block(Statement... statements) {
        return new Block(List.of(statements), RANGE);
    }

    private static IfStatement conditional(Block thenBlock, Statement elseBranch) {
        return new IfStatement(
                new BooleanLiteral(true, RANGE), thenBlock, Optional.ofNullable(elseBranch), RANGE);
    }

    @Test
    void theIndentSizeComesFromTheConfiguration() {
        assertEquals(
                "if (true) {\n  println(1);\n}",
                conditional(block(println(1)), null).accept(visitor(2, true)));
        assertEquals(
                "if (true) {\n        println(1);\n}",
                conditional(block(println(1)), null).accept(visitor(8, true)));
    }

    @Test
    void theBracePlacementComesFromTheConfiguration() {
        assertEquals(
                "if (true)\n{\n    println(1);\n}",
                conditional(block(println(1)), null).accept(visitor(4, false)));
    }

    @Test
    void anElseIfIsPrintedOnTheSameLine() {
        IfStatement inner = conditional(block(println(2)), null);

        assertEquals(
                "if (true) {\n    println(1);\n} else if (true) {\n    println(2);\n}",
                conditional(block(println(1)), inner).accept(visitor(4, true)));
    }

    @Test
    void anElseWithABlockIsNested() {
        IfStatement inner = conditional(block(println(2)), null);

        assertEquals(
                "if (true) {\n    println(1);\n} else {\n    if (true) {\n        println(2);\n    }\n}",
                conditional(block(println(1)), block(inner)).accept(visitor(4, true)));
    }

    @Test
    void anElseWithABlockRespectsTheBracePlacement() {
        assertEquals(
                "if (true)\n{\n    println(1);\n} else\n{\n    println(2);\n}",
                conditional(block(println(1)), block(println(2))).accept(visitor(4, false)));
    }

    @Test
    void anEmptyBlockPrintsNothingBetweenItsBraces() {
        assertEquals("if (true) {\n}", conditional(block(), null).accept(visitor(4, true)));
    }
}
