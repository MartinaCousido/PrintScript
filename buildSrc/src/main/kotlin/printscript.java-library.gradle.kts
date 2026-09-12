// qué es un módulo
// Java 25 + JUnit 5 + repos

plugins {
    id("java-library")
    id("printscript.publish")
}

group = "org.printscript"

// La versión la manda el tag que dispara el CD (-PprintscriptVersion=1.2.3). El default
// es para el trabajo diario: en local nadie publica, así que el número no importa.
version = providers.gradleProperty("printscriptVersion").getOrElse("0.1.0-SNAPSHOT")

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
