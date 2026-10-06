package org.printscript.semantic;

import java.util.Optional;
import java.util.Set;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.Block;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Expression;
import org.printscript.ast.IfStatement;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.Statement;
import org.printscript.ast.StatementOnlyVisitor;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;

// las reglas semánticas
// el estado de error es por sentencia y no acumulado:
// una sentencia inválida no condena a las que siguen
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

        // la tabla decide si ya estaba declarada, y solo mira el scope actual: el mismo
        // nombre en un bloque más interno es sombra, no redeclaración
        Optional<SymbolTable> updated =
                symbols.declare(name, new Symbol(declared.get(), immutable));
        if (updated.isEmpty()) {
            // el rango es el de la declaración entera, como venía reportándolo el
            // intérprete: lo que está de más es la sentencia, no el nombre
            return new Failure<>("Variable ya declarada: " + name, statement);
        }
        // se declara aunque el inicializador esté mal, así un error no arrastra a las
        // sentencias que usan la variable después
        symbols = updated.get();

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

    // un Block es exactamente un scope: se abre un frame al entrar y se cierra al
    // salir, también cuando una sentencia de adentro falló. Es la misma regla para el
    // then, para el else y para el bloque de un else if, porque los tres son Block.
    @Override
    public Result<Void> visitBlock(Block block) {
        symbols = symbols.openScope();
        try {
            for (Statement statement : block.statements()) {
                Result<Void> checked = statement.accept(this);
                if (checked instanceof Failure<Void>) {
                    return checked;
                }
            }
            return success();
        } finally {
            symbols = symbols.closeScope();
        }
    }

    @Override
    public Result<Void> visitIfStatement(IfStatement ifStatement) {
        return ExpressionTyper.typeOf(ifStatement.condition(), symbols)
                .andThen(type -> checkConditionAndBranches(ifStatement, type));
    }

    private Result<Void> checkConditionAndBranches(IfStatement ifStatement, Type type) {
        if (type != Type.BOOLEAN) {
            return new Failure<>(
                    "La condición de un if tiene que ser boolean", ifStatement.condition().range());
        }
        Result<Void> thenChecked = ifStatement.thenBlock().accept(this);
        if (thenChecked instanceof Failure<Void>) {
            return thenChecked;
        }
        return ifStatement
                .elseBranch()
                .map(branch -> branch.accept(this))
                .orElseGet(StatementChecker::success);
    }

    @Override
    public Result<Void> visitPrintlnStatement(PrintlnStatement println) {
        // println acepta cualquier tipo básico: solo hace falta que la expresión tipe
        return ExpressionTyper.typeOf(println.expression(), symbols).map(type -> null);
    }

    private Result<Void> checkAssignable(Expression expression, Type declared) {
        return ExpressionTyper.typeOf(expression, symbols)
                .andThen(actual -> compareTypes(expression, declared, actual));
    }

    private static Result<Void> compareTypes(Expression expression, Type declared, Type actual) {
        // excepción acotada: lo que entra de afuera es texto, y el tipo declarado manda
        // la conversión. Vale solo cuando el inicializador es exactamente una lectura;
        // adentro de una expresión es un string común, o si no deja de ser checkeable.
        if (isExternalRead(expression)) {
            return success();
        }
        if (actual != declared) {
            return new Failure<>(
                    "No se puede asignar "
                            + actual.writtenName()
                            + " a una variable "
                            + declared.writtenName(),
                    expression.range());
        }
        return success();
    }

    private static boolean isExternalRead(Expression expression) {
        return expression instanceof ReadInput || expression instanceof ReadEnv;
    }

    private static Result<Void> success() {
        return new Success<>(null);
    }

    @Override
    protected Result<Void> notAStatement(Range range) {
        return new Failure<>("No es una sentencia", range);
    }
}
