// gradle consulta la api de foojay y descarga el JDK 25
// lo cachea y compila con él; por eso cualquiera puede clonar el repo
// con el java que tenga y compilar igual.
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
    "semantic",
    "runtime-api",
    "interpreter",
    "runner",
    "configuration",
    "linter",
    "formatter",
    "cli",
)
