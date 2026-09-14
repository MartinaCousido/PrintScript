plugins {
    id("printscript.java-library")
    id("printscript.quality")
    id("printscript.format")
}

dependencies {
    api(project(":common"))
    api(project(":token"))
    api(project(":configuration"))

    // solo para los tests: alimentar al formatter con tokens de verdad en vez de
    // construirlos a mano. La dependencia de producción sigue siendo el vocabulario.
    testImplementation(project(":lexer"))
}
