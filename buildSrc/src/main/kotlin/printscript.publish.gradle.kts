// Cómo sale un módulo del repo y queda disponible para que otro proyecto lo consuma.
//
// Vive aparte de printscript.java-library porque esconde una decisión distinta: qué es
// un módulo compilable no es lo mismo que dónde y con qué coordenadas se publica. Si
// mañana se publica a Maven Central en vez de a GitHub Packages, cambia este archivo y
// ningún otro.
plugins {
    id("maven-publish")
}

// El jar de fuentes viaja con el binario: sin él, quien consuma la librería no puede
// leer ni debuggear el código desde su IDE.
// configure<> y no el accesor java { }: los accesores con tipos se generan a partir del
// plugins {} de este mismo archivo, y acá el plugin de Java no está declarado. Lo aplica
// printscript.java-library, que es quien aplica este convention plugin.
configure<JavaPluginExtension> {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ingsis-ua/ingsis-interpreter")
            // En CI los toma del entorno; en local, de ~/.gradle/gradle.properties.
            // Nunca se commitean: publicar desde una máquina es la excepción, no la regla.
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }
    }
}
