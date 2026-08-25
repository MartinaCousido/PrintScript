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
    implementation(project(":interpreter"))
}

tasks.register<JavaExec>("playground") {
    group = "application"
    description = "Corre Playground.java para probar programas PrintScript a mano"
    mainClass.set("org.printscript.runner.Playground")
    classpath = sourceSets["main"].runtimeClasspath
}
