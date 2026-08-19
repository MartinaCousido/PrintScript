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

    // Solo se formatean los archivos modificados respecto de master. Sin esto,
    // instalar Spotless reformatea los 83 archivos de una: un commit de ~1900
    // líneas que tapa el historial y le deja conflictos a todo el equipo.
    // El formato se adopta de a poco, a medida que cada archivo se toca.
    //
    // Requiere historia de git completa: el CI clona con fetch-depth 0 por esto.
    ratchetFrom("origin/master")
}
