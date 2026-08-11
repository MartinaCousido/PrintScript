package org.printscript.lexer;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

public final class Lexer implements PrintScriptIterator<Token> {

    private final Iterator<String> lines;
    private final List<TokenConstructor> constructors;
    private String currentLine;
    private int lineNumber;
    private int columnIndex;
    private Token pending;

    public Lexer(Iterator<String> lines, List<TokenConstructor> constructors) {
        this.lines = lines;
        this.constructors = constructors;
        this.currentLine = null;
        this.lineNumber = 0;
        this.columnIndex = 0;
        this.pending = null;
    }

    @Override
    public boolean hasNext() {
        if (pending == null) {
            pending = advance();
        }
        return pending != null;
    }

    @Override
    public Token getNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Token token = pending;
        pending = null;
        return token;
    }

    private Token advance() {
        while (true) {
            if (currentLine == null || columnIndex >= currentLine.length()) {
                if (!lines.hasNext()) {
                    return null;
                }
                currentLine = lines.next();
                lineNumber = lineNumber + 1;
                columnIndex = 0;
                continue;
            }
            char character = currentLine.charAt(columnIndex);
            if (character == ' ' || character == '\t') {
                columnIndex = columnIndex + 1;
                continue;
            }
            return buildTokenAtCurrentColumn();
        }
    }

    private Token buildTokenAtCurrentColumn() {
        Position start = new Position(lineNumber, columnIndex + 1);
        String rest = currentLine.substring(columnIndex);
        Token longest = null;
        int longestLength = 0;
        for (TokenConstructor constructor : constructors) {
            Optional<Token> candidate = constructor.tryBuild(rest, start);
            if (candidate.isEmpty()) {
                continue;
            }
            int length = lengthOf(candidate.get());
            if (length > longestLength) {
                longest = candidate.get();
                longestLength = length;
            }
        }
        if (longest == null) {
            return unknownAtCurrentColumn();
        }
        columnIndex = columnIndex + longestLength;
        return longest;
    }

    private static int lengthOf(Token token) {
        return token.range().end().column() - token.range().start().column() + 1;
    }

    private Token unknownAtCurrentColumn() {
        Position start = new Position(lineNumber, columnIndex + 1);
        String lexeme = String.valueOf(currentLine.charAt(columnIndex));
        columnIndex = columnIndex + 1;
        return new Token(TokenType.UNKNOWN, lexeme, new Range(start, start));
    }
}
