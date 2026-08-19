plugins {
    id("com.diffplug.spotless")
}

group = "org.printscript"
version = "0.1.0"

// El convention plugin trae los repositorios, pero lo aplican los ocho módulos
// y no el raíz: ktlint se baja desde acá, así que el raíz necesita el suyo.
repositories {
    mavenCentral()
}

// Los archivos de build no los formateaba nadie: el convention plugin
// printscript.format lo aplican los ocho módulos y solo alcanza a los .java.
// Esta configuración vive en el raíz porque desde acá se llega a todos los
// .gradle.kts del repo, incluidos los de buildSrc, que es un build separado
// y no lo alcanza ninguna otra configuración.
//
// Sin ratchetFrom, a diferencia del convention plugin de los .java. Ahí el
// ratchet evita un reformateo de 83 archivos; acá los 16 archivos de build y
// texto YA cumplen, así que no hay nada que absorber de a poco y conviene la
// cobertura completa desde el día uno.
spotless {
    kotlinGradle {
        target(
            "*.gradle.kts",
            "*/*.gradle.kts",
            "buildSrc/*.gradle.kts",
            "buildSrc/src/main/kotlin/*.gradle.kts",
        )
        // Versión fijada, igual que checkstyle, jacoco y google-java-format.
        //
        // El indent va explícito porque el paso de ktlint de Spotless NO toma el
        // valor del .editorconfig: sin esto reindenta los .kts de 4 a 2 espacios.
        // Tiene que seguir coincidiendo con lo que declara el .editorconfig, que
        // es lo que lee el IDE.
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
