package org.printscript.common;

// qué regla del lenguaje se incumplió: el mensaje lo explica a una persona y el
// código lo identifica para un programa, aunque el texto del mensaje cambie
public enum ErrorCode {
    // sintaxis
    MISSING_SEMICOLON,
    UNCLOSED_BLOCK,
    UNRECOGNIZED_STATEMENT,
    MISSING_OPERAND,
    UNCLOSED_PARENTHESIS,
    EXPECTED_OPENING_PARENTHESIS,
    EXPECTED_CLOSING_PARENTHESIS,
    EXPECTED_OPENING_BRACE,
    EXPECTED_ASSIGNMENT,
    EXPECTED_IDENTIFIER,
    EXPECTED_COLON,
    EXPECTED_TYPE,
    INVALID_NUMBER_LITERAL,
    UNEXPECTED_TOKEN,
    UNKNOWN_SYMBOL,

    // semántica
    CONST_WITHOUT_VALUE,
    UNKNOWN_TYPE,
    VARIABLE_ALREADY_DECLARED,
    UNDECLARED_VARIABLE,
    CONST_REASSIGNMENT,
    NON_BOOLEAN_CONDITION,
    TYPE_MISMATCH,
    INVALID_ARGUMENT_TYPE,
    INVALID_OPERATION,

    // ejecución
    NOT_A_NUMBER,
    NOT_A_BOOLEAN,
    MISSING_INPUT,
    MISSING_ENVIRONMENT_VARIABLE,

    // formato
    SPACING_TOO_LARGE,

    // un nodo que la etapa no sabe procesar: indica un error interno, no del programa
    UNSUPPORTED_NODE
}
