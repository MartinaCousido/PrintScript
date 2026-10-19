plugins {
    id("printscript.java-library")
    id("printscript.quality")
    id("printscript.format")
    application
}

application {
    mainClass.set("org.printscript.cli.Cli")
    applicationName = "printscript"
}

// la interfaz interactiva (sin argumentos) lee de stdin; sin esto, `gradlew :cli:run`
// no conecta la entrada estándar del proceso Gradle al de la aplicación
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    workingDir = rootProject.projectDir
}

dependencies {
    implementation(project(":runner"))
    implementation(project(":runtime-api"))
    implementation(project(":common"))
    implementation("info.picocli:picocli:4.7.7")
}
