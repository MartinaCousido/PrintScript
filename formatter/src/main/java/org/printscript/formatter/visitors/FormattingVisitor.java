package org.printscript.formatter.visitors;

import java.math.BigDecimal;
import org.printscript.ast.ASTVisitor;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.VariableDeclaration;
import org.printscript.formatter.config.Separators;
import org.printscript.formatter.visitors.Parentheses.Side;

public final class FormattingVisitor implements ASTVisitor<String> {

    private static final String END_OF_STATEMENT = ";";

    private final Separators separators;

    public FormattingVisitor(Separators separators) {
        this.separators = separators;
    }

    @Override
    public String visitNumberLiteral(NumberLiteral numberLiteral) {
        return BigDecimal.valueOf(numberLiteral.value()).stripTrailingZeros().toPlainString();
    }

    @Override
    public String visitStringLiteral(StringLiteral stringLiteral) {
        return "\"" + stringLiteral.value() + "\"";
    }

    @Override
    public String visitIdentifier(Identifier identifier) {
        return identifier.name();
    }

    @Override
    public String visitBinaryExpression(BinaryExpression binaryExpression) {
        Operator operator = binaryExpression.operator();
        return operand(binaryExpression.left(), operator, Side.LEFT)
                + " "
                + symbolOf(operator)
                + " "
                + operand(binaryExpression.right(), operator, Side.RIGHT);
    }

    @Override
    public String visitVariableDeclaration(VariableDeclaration variableDeclaration) {
        String declared =
                "let "
                        + variableDeclaration.name().accept(this)
                        + separators.colon()
                        + variableDeclaration.type().name();
        String initialized =
                variableDeclaration
                        .initializer()
                        .map(initializer -> separators.assign() + initializer.accept(this))
                        .orElse("");
        return declared + initialized + END_OF_STATEMENT;
    }

    @Override
    public String visitAssignmentStatement(AssignmentStatement assignmentStatement) {
        return assignmentStatement.name().accept(this)
                + separators.assign()
                + assignmentStatement.expression().accept(this)
                + END_OF_STATEMENT;
    }

    @Override
    public String visitPrintlnStatement(PrintlnStatement printlnStatement) {
        return "println(" + printlnStatement.expression().accept(this) + ")" + END_OF_STATEMENT;
    }

    private String operand(Expression child, Operator parent, Side side) {
        String text = child.accept(this);
        return Parentheses.areNeededAround(child, parent, side) ? "(" + text + ")" : text;
    }

    private static String symbolOf(Operator operator) {
        return switch (operator) {
            case PLUS -> "+";
            case MINUS -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
        };
    }
}
