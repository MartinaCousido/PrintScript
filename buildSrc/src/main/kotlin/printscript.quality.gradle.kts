plugins {
    id("jacoco")
    id("checkstyle")
}

// Versiones fijadas, no "la última": el mismo código con las mismas herramientas
// tiene que dar el mismo resultado en cualquier máquina y en cualquier momento.
jacoco {
    toolVersion = "0.8.14"
}

checkstyle {
    toolVersion = "10.21.0"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    violationRules {
        rule {
            // El piso está por debajo de la cobertura real para que avise cuando
            // baja, no para que moleste. Coverage mide qué líneas se ejecutaron,
            // no si se verificó algo: es un piso, nunca un objetivo.
            limit {
                counter = "INSTRUCTION"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
