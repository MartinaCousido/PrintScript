// formato de los .java

plugins {
    id("com.diffplug.spotless")
}

// Versiones fijadas, no "la última": el mismo código con las mismas
// herramientas tiene que dar el mismo resultado en cualquier máquina.
spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.27.0").aosp()
        removeUnusedImports()
    }

    // Requiere historia de git completa: el CI clona con fetch-depth 0 por esto.
    ratchetFrom("origin/master")
}
