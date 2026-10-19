plugins {
    id("com.diffplug.spotless")
    id("jacoco")
}

allprojects {
    dependencyLocking {
        lockAllConfigurations()
        lockMode.set(LockMode.STRICT)
    }
}

group = "org.printscript"
version = providers.gradleProperty("printscriptVersion").getOrElse("0.1.0-SNAPSHOT")

jacoco {
    toolVersion = "0.8.14"
}

// el task hace un reporte de cobertura unificado de los 13 módulos
tasks.register<JacocoReport>("jacocoRootReport") {
    group = "verification"
    description = "Genera el reporte de cobertura de todos los módulos"
    dependsOn(subprojects.map { it.tasks.named("test") })
    executionData(subprojects.map { it.fileTree(it.layout.buildDirectory).include("jacoco/test.exec") })
    sourceDirectories.from(subprojects.map { it.file("src/main/java") })
    classDirectories.from(subprojects.map { it.fileTree(it.layout.buildDirectory.dir("classes/java/main")) })
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// El convention plugin trae los repositorios, pero lo aplican los trece módulos
// y no el raíz: ktlint se baja desde acá, así que el raíz necesita el suyo.
repositories {
    mavenCentral()
}

val cpd: Configuration by configurations.creating
dependencies {
    cpd("net.sourceforge.pmd:pmd-cli:7.28.0")
    cpd("net.sourceforge.pmd:pmd-java:7.28.0")
}

tasks.register<JavaExec>("cpdCheck") {
    group = "verification"
    description = "Detecta código Java duplicado entre módulos"
    val reporte = layout.buildDirectory.file("reports/cpd/cpd.txt")
    outputs.file(reporte)
    // Las fuentes entran como strings en args, así que Gradle no las ve como inputs.
    // Sin declararlas, el task queda UP-TO-DATE apenas existe el reporte y CPD deja de
    // correr aunque cambie el código: la duplicación solo se detectaría en un build limpio.
    subprojects.forEach {
        inputs.dir(it.file("src/main/java")).withPathSensitivity(PathSensitivity.RELATIVE)
    }
    classpath = cpd
    mainClass.set("net.sourceforge.pmd.cli.PmdCli")
    args("cpd", "--minimum-tokens", "50", "--language", "java")
    subprojects.forEach { args("--dir", "${it.name}/src/main/java") }
    doFirst {
        val archivo = reporte.get().asFile
        archivo.parentFile.mkdirs()
        standardOutput = archivo.outputStream()
    }
}

tasks.named("check") {
    dependsOn("cpdCheck")
}

// le dice a git que busque los hooks en config/githooks en vez de en .git/hooks (que no se clona)
tasks.register<Exec>("installGitHooks") {
    group = "development"
    description = "Instala el hook de pre-commit en este clon"
    commandLine("git", "config", "core.hooksPath", "config/githooks")
}

// formatea kotlinGradle y format("textos")
spotless {
    kotlinGradle {
        target(
            "*.gradle.kts",
            "*/*.gradle.kts",
            "buildSrc/*.gradle.kts",
            "buildSrc/src/main/kotlin/*.gradle.kts",
        )
        ktlint("1.5.0").editorConfigOverride(mapOf("indent_size" to "4"))
    }

    // Sin motor: solo recorta espacios al final y garantiza el salto final.
    // Es poco, pero es lo que evita los diffs sucios en los archivos que más
    // se editan a mano.
    format("textos") {
        target("**/*.md", "**/*.yml", "**/*.yaml")
        targetExclude("**/build/**", "**/.gradle/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
