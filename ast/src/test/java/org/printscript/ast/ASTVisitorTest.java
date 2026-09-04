package org.printscript.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;

class ASTVisitorTest {

    private static Range rangeAt(int startColumn, int endColumn) {
        return new Range(new Position(1, startColumn), new Position(1, endColumn));
    }

    private static final class SourceWriter implements ASTVisitor<String> {

        @Override
        public String visitNumberLiteral(NumberLiteral numberLiteral) {
            double value = numberLiteral.value();
            if (value == Math.rint(value) && !Double.isInfinite(value)) {
                return Long.toString((long) value);
            }
            return Double.toString(value);
        }

        @Override
        public String visitStringLiteral(StringLiteral stringLiteral) {
            return "\"" + stringLiteral.value() + "\"";
        }

        @Override
        public String visitBooleanLiteral(BooleanLiteral booleanLiteral) {
            return Boolean.toString(booleanLiteral.value());
        }

        @Override
        public String visitIdentifier(Identifier identifier) {
            return identifier.name();
        }

        @Override
        public String visitBinaryExpression(BinaryExpression binaryExpression) {
            return "("
                    + binaryExpression.left().accept(this)
                    + " "
                    + symbol(binaryExpression.operator())
                    + " "
                    + binaryExpression.right().accept(this)
                    + ")";
        }

        @Override
        public String visitVariableDeclaration(VariableDeclaration variableDeclaration) {
            String head =
                    "let "
                            + variableDeclaration.name().name()
                            + ": "
                            + variableDeclaration.type().name();
            return variableDeclaration
                    .initializer()
                    .map(expression -> head + " = " + expression.accept(this) + ";")
                    .orElse(head + ";");
        }

        @Override
        public String visitAssignmentStatement(AssignmentStatement assignmentStatement) {
            return assignmentStatement.name().name()
                    + " = "
                    + assignmentStatement.expression().accept(this)
                    + ";";
        }

        @Override
        public String visitPrintlnStatement(PrintlnStatement printlnStatement) {
            return "println(" + printlnStatement.expression().accept(this) + ");";
        }

        private static String symbol(Operator operator) {
            return switch (operator) {
                case PLUS -> "+";
                case MINUS -> "-";
                case MULTIPLY -> "*";
                case DIVIDE -> "/";
            };
        }
    }

    @Test
    void visitorRebuildsANestedExpressionAsText() {
        Expression tree =
                new BinaryExpression(
                        new NumberLiteral(3.0, rangeAt(1, 1)),
                        Operator.PLUS,
                        new BinaryExpression(
                                new Identifier("x", rangeAt(5, 5)),
                                Operator.MULTIPLY,
                                new NumberLiteral(2.0, rangeAt(9, 9)),
                                rangeAt(5, 9)),
                        rangeAt(1, 9));

        assertEquals("(3 + (x * 2))", tree.accept(new SourceWriter()));
    }

    @Test
    void visitorRebuildsADeclarationWithInitializer() {
        Statement declaration =
                new VariableDeclaration(
                        new Identifier("total", rangeAt(5, 9)),
                        new TypeName("number", rangeAt(12, 17)),
                        Optional.of(
                                new BinaryExpression(
                                        new NumberLiteral(12.0, rangeAt(21, 22)),
                                        Operator.DIVIDE,
                                        new NumberLiteral(4.0, rangeAt(26, 26)),
                                        rangeAt(21, 26))),
                        rangeAt(1, 26));

        assertEquals("let total: number = (12 / 4);", declaration.accept(new SourceWriter()));
    }

    @Test
    void visitorRebuildsADeclarationWithoutInitializer() {
        Statement declaration =
                new VariableDeclaration(
                        new Identifier("name", rangeAt(5, 8)),
                        new TypeName("string", rangeAt(11, 16)),
                        Optional.empty(),
                        rangeAt(1, 16));

        assertEquals("let name: string;", declaration.accept(new SourceWriter()));
    }

    @Test
    void visitorRebuildsAssignmentAndPrintln() {
        Statement assignment =
                new AssignmentStatement(
                        new Identifier("x", rangeAt(1, 1)),
                        new StringLiteral("hi", rangeAt(5, 8)),
                        rangeAt(1, 8));
        Statement println =
                new PrintlnStatement(new Identifier("x", rangeAt(9, 9)), rangeAt(1, 10));

        assertEquals("x = \"hi\";", assignment.accept(new SourceWriter()));
        assertEquals("println(x);", println.accept(new SourceWriter()));
    }
}
