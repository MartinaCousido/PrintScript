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

tasks.register<JavaExec>("playground") {
    group = "application"
    description = "Corre Playground.java para probar programas PrintScript a mano"
    mainClass.set("org.printscript.runner.Playground")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.named<Pmd>("pmdMain") {
    exclude("**/Playground.java")
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    classDirectories.setFrom(
        classDirectories.files.map {
            // también las clases anidadas del Playground, que no son parte del sistema
            fileTree(it) { exclude("**/Playground.class", "**/Playground$*.class") }
        },
    )
}
