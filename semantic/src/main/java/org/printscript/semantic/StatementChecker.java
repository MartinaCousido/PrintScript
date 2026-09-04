package org.printscript.semantic;

import java.util.Optional;
import java.util.Set;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.Block;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Expression;
import org.printscript.ast.IfStatement;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.StatementOnlyVisitor;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;

// las reglas semánticas de PrintScript 1.0
// el estado de error es por sentencia y no acumulado: una sentencia inválida no
// condena a las que siguen. Lo único que se arrastra es la tabla de tipos.
public final class StatementChecker extends StatementOnlyVisitor<Result<Void>> {

    private final Set<Type> supportedTypes;
    private SymbolTable symbols;

    public StatementChecker(Set<Type> supportedTypes) {
        this.supportedTypes = Set.copyOf(supportedTypes);
        this.symbols = new SymbolTable();
    }

    @Override
    public Result<Void> visitVariableDeclaration(VariableDeclaration declaration) {
        return declare(
                declaration.name().name(),
                declaration.type(),
                declaration.initializer(),
                declaration.range(),
                false);
    }

    @Override
    public Result<Void> visitConstDeclaration(ConstDeclaration declaration) {
        if (declaration.initializer().isEmpty()) {
            // una constante sin valor no se puede completar después: nace inservible
            return new Failure<>(
                    "Una constante tiene que declararse con un valor", declaration.range());
        }
        return declare(
                declaration.name().name(),
                declaration.type(),
                declaration.initializer(),
                declaration.range(),
                true);
    }

    private Result<Void> declare(
            String name,
            TypeName annotation,
            Optional<Expression> initializer,
            Range statement,
            boolean immutable) {
        Optional<Type> declared = Type.fromWrittenName(annotation.name(), supportedTypes);
        if (declared.isEmpty()) {
            return new Failure<>("Tipo desconocido: " + annotation.name(), annotation.range());
        }

        if (symbols.read(name).isPresent()) {
            // el rango es el de la declaración entera, como venía reportándolo el
            // intérprete: lo que está de más es la sentencia, no el nombre
            return new Failure<>("Variable ya declarada: " + name, statement);
        }
        // se declara aunque el inicializador esté mal, así un error no arrastra a las
        // sentencias que usan la variable después
        symbols = symbols.declare(name, new Symbol(declared.get(), immutable)).orElseThrow();

        if (initializer.isEmpty()) {
            return success();
        }
        return checkAssignable(initializer.get(), declared.get());
    }

    @Override
    public Result<Void> visitAssignmentStatement(AssignmentStatement assignment) {
        String name = assignment.name().name();
        Optional<Symbol> declared = symbols.read(name);
        if (declared.isEmpty()) {
            // en una asignación la sentencia entera es la que no tiene sentido; cuando
            // el nombre aparece dentro de una expresión, el rango es el del uso
            return new Failure<>("Variable no declarada: " + name, assignment.range());
        }
        if (declared.get().immutable()) {
            return new Failure<>("No se puede reasignar la constante " + name, assignment.range());
        }
        return checkAssignable(assignment.expression(), declared.get().type());
    }

    @Override
    public Result<Void> visitBlock(Block block) {
        return notCheckedYet(block.range());
    }

    @Override
    public Result<Void> visitIfStatement(IfStatement ifStatement) {
        return notCheckedYet(ifStatement.range());
    }

    // el scope de bloque y la regla de la condición booleana entran con el if; hasta
    // entonces el error viaja como dato en vez de dejar pasar algo sin validar
    private static Result<Void> notCheckedYet(Range range) {
        return new Failure<>("semantic todavía no valida bloques ni if", range);
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

    private static Result<Void> success() {
        return new Success<>(null);
    }

    private static Result<Void> propagate(Failure<Type> failure) {
        return new Failure<>(failure.message(), failure.range());
    }

    @Override
    protected Result<Void> notAStatement(Range range) {
        return new Failure<>("No es una sentencia", range);
    }
}
