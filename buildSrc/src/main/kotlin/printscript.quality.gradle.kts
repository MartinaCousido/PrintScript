plugins {
    id("jacoco")
    id("checkstyle")
    id("pmd")
}

// Versiones fijadas, no "la última": el mismo código con las mismas herramientas
// tiene que dar el mismo resultado en cualquier máquina y en cualquier momento.
jacoco {
    toolVersion = "0.8.14"
}

checkstyle {
    toolVersion = "10.21.0"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    // Sin maxWarnings: el checkstyle.xml declara severity=error global, asi que
    // ninguna violacion es warning nunca y esa propiedad no cambiaba nada.
    isIgnoreFailures = false
}

pmd {
    toolVersion = "7.26.0"
    ruleSetFiles = files(rootProject.file("config/pmd/ruleset.xml"))
    // Sin esto, el plugin suma su ruleset por defecto al nuestro y aparecen
    // violaciones de reglas que nadie eligió.
    ruleSets = emptyList()
    isIgnoreFailures = false
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

val pisosDeCobertura = mapOf(
    "common" to "0.95",
    "token" to "0.95",
    "ast" to "0.95",
    "lexer" to "0.95",
    "parser" to "0.90",
    "runner" to "0.95",
    "configuration" to "0.95",
    "linter" to "0.95",
)

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    violationRules {
        rule {
            // El piso está por debajo de la cobertura real para que avise cuando
            // baja, no para que moleste. Coverage mide qué líneas se ejecutaron,
            // no si se verificó algo: es un piso, nunca un objetivo.
            limit {
                counter = "INSTRUCTION"
                minimum = (pisosDeCobertura[project.name] ?: "0.80").toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
