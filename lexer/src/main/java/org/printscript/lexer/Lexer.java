package org.printscript.lexer;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.printscript.common.Position;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

public final class Lexer implements PrintScriptIterator<Token> {

    private final Iterator<String> lines;
    private String currentLine;
    private int lineNumber;
    private int columnIndex;
    private Token pending;

    public Lexer(Iterator<String> lines) {
        this.lines = lines;
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
            return unknownAtCurrentColumn();
        }
    }

    private Token unknownAtCurrentColumn() {
        Position start = new Position(lineNumber, columnIndex + 1);
        String lexeme = String.valueOf(currentLine.charAt(columnIndex));
        columnIndex = columnIndex + 1;
        return new Token(TokenType.UNKNOWN, lexeme, new Range(start, start));
    }
}
