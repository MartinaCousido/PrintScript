package org.printscript.semantic;

import java.util.Optional;
import java.util.Set;
import org.printscript.ast.ASTVisitor;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;

// las reglas semánticas de PrintScript 1.0
// el estado de error es por sentencia y no acumulado: una sentencia inválida no
// condena a las que siguen. Lo único que se arrastra es la tabla de tipos.
public final class StatementChecker implements ASTVisitor<Result<Void>> {

    private final Set<Type> supportedTypes;
    private SymbolTable symbols;

    public StatementChecker(Set<Type> supportedTypes) {
        this.supportedTypes = Set.copyOf(supportedTypes);
        this.symbols = new SymbolTable();
    }

    @Override
    public Result<Void> visitVariableDeclaration(VariableDeclaration declaration) {
        Optional<Type> declared = Type.fromWrittenName(declaration.type().name(), supportedTypes);
        if (declared.isEmpty()) {
            return new Failure<>(
                    "Tipo desconocido: " + declaration.type().name(), declaration.type().range());
        }

        String name = declaration.name().name();
        if (symbols.read(name).isPresent()) {
            // el rango es el de la declaración entera, como venía reportándolo el
            // intérprete: lo que está de más es la sentencia, no el nombre
            return new Failure<>("Variable ya declarada: " + name, declaration.range());
        }
        // se declara aunque el inicializador esté mal, así un error no arrastra a las
        // sentencias que usan la variable después
        symbols = symbols.declare(name, declared.get()).orElseThrow();

        Optional<Expression> initializer = declaration.initializer();
        if (initializer.isEmpty()) {
            return success();
        }
        return checkAssignable(initializer.get(), declared.get());
    }

    @Override
    public Result<Void> visitAssignmentStatement(AssignmentStatement assignment) {
        String name = assignment.name().name();
        Optional<Type> declared = symbols.read(name);
        if (declared.isEmpty()) {
            // en una asignación la sentencia entera es la que no tiene sentido; cuando
            // el nombre aparece dentro de una expresión, el rango es el del uso
            return new Failure<>("Variable no declarada: " + name, assignment.range());
        }
        return checkAssignable(assignment.expression(), declared.get());
    }

    @Override
    public Result<Void> visitPrintlnStatement(PrintlnStatement println) {
        // println acepta cualquier tipo básico: solo hace falta que la expresión tipe
        Result<Type> actual = ExpressionTyper.typeOf(println.expression(), symbols);
        if (actual instanceof Failure<Type> failure) {
            return propagate(failure);
        }
        return success();
    }

    private Result<Void> checkAssignable(Expression expression, Type declared) {
        Result<Type> actual = ExpressionTyper.typeOf(expression, symbols);
        if (actual instanceof Failure<Type> failure) {
            return propagate(failure);
        }
        Type type = ((Success<Type>) actual).value();
        if (type != declared) {
            return new Failure<>(
                    "No se puede asignar "
                            + type.writtenName()
                            + " a una variable "
                            + declared.writtenName(),
                    expression.range());
        }
        return success();
    }

    @Override
    public Result<Void> visitNumberLiteral(NumberLiteral numberLiteral) {
        return notAStatement(numberLiteral.range());
    }

    @Override
    public Result<Void> visitStringLiteral(StringLiteral stringLiteral) {
        return notAStatement(stringLiteral.range());
    }

    @Override
    public Result<Void> visitIdentifier(Identifier identifier) {
        return notAStatement(identifier.range());
    }

    @Override
    public Result<Void> visitBinaryExpression(BinaryExpression binaryExpression) {
        return notAStatement(binaryExpression.range());
    }

    private static Result<Void> success() {
        return new Success<>(null);
    }

    private static Result<Void> propagate(Failure<Type> failure) {
        return new Failure<>(failure.message(), failure.range());
    }

    private static Result<Void> notAStatement(Range range) {
        return new Failure<>("No es una sentencia", range);
    }
}
