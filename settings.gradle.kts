plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ingsis-interpreter"

include(
    "common",
    "token",
    "ast",
    "lexer",
    "parser",
    "runtime-api",
    "interpreter",
    "runner",
    "configuration",
    "linter",
    "formatter",
)
