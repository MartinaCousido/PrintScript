plugins {
    id("printscript.java-library")
    id("printscript.quality")
    id("printscript.format")
}

dependencies {
    api(project(":common"))
    api(project(":runtime-api"))
    implementation(project(":lexer"))
    implementation(project(":parser"))
    implementation(project(":semantic"))
    implementation(project(":interpreter"))
    api(project(":configuration"))
    api(project(":linter"))
    api(project(":formatter"))
}

// Ejemplo de desarrollo: se compila con los tests y no se publica en el jar.
tasks.register<JavaExec>("playground") {
    group = "development"
    description = "Ejecuta el ejemplo de desarrollo, sin incluirlo en la librería publicada"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("org.printscript.runner.Playground")
    classpath = sourceSets["test"].runtimeClasspath
}
