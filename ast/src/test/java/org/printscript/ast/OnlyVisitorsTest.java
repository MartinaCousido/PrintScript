package org.printscript.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;

// las dos bases existen para que el rechazo de "esto no me toca" se escriba una sola
// vez. Estos tests fijan que ninguna de las dos se olvide de un nodo: cuando el AST
// gane una construcción nueva, acá se ve enseguida cuál de las dos hay que completar.
class OnlyVisitorsTest {

    private static final Range RANGE = new Range(new Position(1, 1), new Position(1, 4));

    private static final class RejectingStatements extends StatementOnlyVisitor<String> {

        @Override
        protected String notAStatement(Range range) {
            return "rechazada:" + range.start().column();
        }

        @Override
        public String visitVariableDeclaration(VariableDeclaration declaration) {
            return "let";
        }

        @Override
        public String visitConstDeclaration(ConstDeclaration declaration) {
            return "const";
        }

        @Override
        public String visitAssignmentStatement(AssignmentStatement assignment) {
            return "asignacion";
        }

        @Override
        public String visitPrintlnStatement(PrintlnStatement println) {
            return "println";
        }

        @Override
        public String visitBlock(Block block) {
            return "bloque";
        }

        @Override
        public String visitIfStatement(IfStatement ifStatement) {
            return "if";
        }
    }

    private static final class RejectingExpressions extends ExpressionOnlyVisitor<String> {

        @Override
        protected String notAnExpression(Range range) {
            return "rechazada:" + range.start().column();
        }

        @Override
        public String visitNumberLiteral(NumberLiteral literal) {
            return "number";
        }

        @Override
        public String visitStringLiteral(StringLiteral literal) {
            return "string";
        }

        @Override
        public String visitBooleanLiteral(BooleanLiteral literal) {
            return "boolean";
        }

        @Override
        public String visitIdentifier(Identifier identifier) {
            return "identificador";
        }

        @Override
        public String visitBinaryExpression(BinaryExpression binary) {
            return "binaria";
        }
    }

    private static List<Expression> everyExpression() {
        return List.of(
                new NumberLiteral(1, RANGE),
                new StringLiteral("x", RANGE),
                new BooleanLiteral(true, RANGE),
                new Identifier("x", RANGE),
                new BinaryExpression(
                        new NumberLiteral(1, RANGE),
                        Operator.PLUS,
                        new NumberLiteral(2, RANGE),
                        RANGE));
    }

    private static List<Statement> everyStatement() {
        Identifier name = new Identifier("x", RANGE);
        TypeName type = new TypeName("number", RANGE);
        Block block = new Block(List.of(), RANGE);
        return List.of(
                new VariableDeclaration(name, type, Optional.empty(), RANGE),
                new ConstDeclaration(name, type, Optional.empty(), RANGE),
                new AssignmentStatement(name, new NumberLiteral(1, RANGE), RANGE),
                new PrintlnStatement(new NumberLiteral(1, RANGE), RANGE),
                block,
                new IfStatement(new BooleanLiteral(true, RANGE), block, Optional.empty(), RANGE));
    }

    @Test
    void aStatementOnlyVisitorRejectsEveryExpressionKeepingItsRange() {
        RejectingStatements visitor = new RejectingStatements();

        for (Expression expression : everyExpression()) {
            assertEquals("rechazada:1", expression.accept(visitor));
        }
    }

    @Test
    void aStatementOnlyVisitorStillHandlesEveryStatement() {
        RejectingStatements visitor = new RejectingStatements();

        assertEquals(
                List.of("let", "const", "asignacion", "println", "bloque", "if"),
                everyStatement().stream().map(statement -> statement.accept(visitor)).toList());
    }

    @Test
    void anExpressionOnlyVisitorRejectsEveryStatementKeepingItsRange() {
        RejectingExpressions visitor = new RejectingExpressions();

        for (Statement statement : everyStatement()) {
            assertEquals("rechazada:1", statement.accept(visitor));
        }
    }

    @Test
    void anExpressionOnlyVisitorStillHandlesEveryExpression() {
        RejectingExpressions visitor = new RejectingExpressions();

        assertEquals(
                List.of("number", "string", "boolean", "identificador", "binaria"),
                everyExpression().stream().map(expression -> expression.accept(visitor)).toList());
    }
}
