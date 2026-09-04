package org.printscript.formatter.visitors;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Collectors;
import org.printscript.ast.ASTVisitor;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Block;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.IfStatement;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.formatter.config.Separators;
import org.printscript.formatter.visitors.Parentheses.Side;

public final class FormattingVisitor implements ASTVisitor<String> {

    private static final String ONE_LEVEL = "    ";
    private static final String LINE_BREAK = "\n";

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
    public String visitBooleanLiteral(BooleanLiteral booleanLiteral) {
        return Boolean.toString(booleanLiteral.value());
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
        return declaration(
                "let",
                variableDeclaration.name(),
                variableDeclaration.type(),
                variableDeclaration.initializer());
    }

    @Override
    public String visitConstDeclaration(ConstDeclaration constDeclaration) {
        return declaration(
                "const",
                constDeclaration.name(),
                constDeclaration.type(),
                constDeclaration.initializer());
    }

    // Provisional: la sangría es fija y la llave va siempre en la misma línea. Es lo
    // que se puede hacer sin canal de error (este visitor devuelve String, no Result) y
    // sin claves de configuración, que entran en la tarea 15 junto con la distinción
    // entre "else if" y "else { if }".
    @Override
    public String visitBlock(Block block) {
        return block.statements().stream()
                .map(statement -> indent(statement.accept(this)))
                .collect(Collectors.joining());
    }

    @Override
    public String visitIfStatement(IfStatement ifStatement) {
        String head =
                "if ("
                        + ifStatement.condition().accept(this)
                        + ") {"
                        + LINE_BREAK
                        + ifStatement.thenBlock().accept(this)
                        + "}";
        return head
                + ifStatement
                        .elseBranch()
                        .map(branch -> " else {" + LINE_BREAK + indent(branch.accept(this)) + "}")
                        .orElse("");
    }

    private static String indent(String text) {
        return text.lines()
                .map(line -> ONE_LEVEL + line + LINE_BREAK)
                .collect(Collectors.joining());
    }

    private String declaration(
            String declarator, Identifier name, TypeName type, Optional<Expression> initializer) {
        String declared = declarator + " " + name.accept(this) + separators.colon() + type.name();
        String initialized =
                initializer
                        .map(expression -> separators.assign() + expression.accept(this))
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
