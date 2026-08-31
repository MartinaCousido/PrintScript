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

dependencies {
    implementation(project(":runner"))
    implementation(project(":runtime-api"))
    implementation(project(":common"))
    implementation("info.picocli:picocli:4.7.6")
}
