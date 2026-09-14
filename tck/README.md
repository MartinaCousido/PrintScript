# Correr el TCK

El [printscript-tck](https://github.com/austral-ingsis/printscript-tck) es un repositorio
aparte: define la interfaz `PrintScriptFactory` y corre sus casos contra cualquier
implementación que la cumpla. El adapter tiene que vivir allá porque implementa interfaces
que son del TCK, no de PrintScript; acá se versionan los fuentes para que no se pierdan y
para que se revisen junto con el resto del código.

## Los dos archivos

| Archivo | Qué hace |
|---|---|
| `implementation/CustomImplementationFactory.java` | las tres operaciones, cada una entrando por su fachada del módulo `runner` |
| `implementation/PrintScriptAdapters.java` | la traducción de vocabularios: `InputStream` a `Iterator<String>`, el `String` de versión a `Version`, y los puertos |

No tienen lógica de lenguaje. Si hiciera falta tocarlos para que un caso pase, el cambio
va en el módulo que corresponda, no acá.

## Pasos

```sh
# 1. publicar la implementación donde el TCK la pueda resolver
./gradlew publishToMavenLocal -PprintscriptVersion=0.2.0-SNAPSHOT

# 2. clonar el TCK y copiar el adapter
git clone https://github.com/austral-ingsis/printscript-tck
cp tck/implementation/*.java printscript-tck/src/main/java/implementation/
rm printscript-tck/src/main/java/implementation/NotImplementedException.java

# 3. correr
cd printscript-tck && ./gradlew test
```

## Tres ajustes que necesita el `build.gradle` del TCK

```groovy
java {
    // PrintScript compila con el toolchain 25; con 21 las clases no cargan
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

dependencies {
    testImplementation 'junit:junit:4.13.1'
    implementation 'org.printscript:runner:0.2.0-SNAPSHOT'
    implementation 'org.printscript:runtime-api:0.2.0-SNAPSHOT'
    implementation 'org.printscript:common:0.2.0-SNAPSHOT'
    implementation 'org.printscript:configuration:0.2.0-SNAPSHOT'
    implementation 'org.printscript:linter:0.2.0-SNAPSHOT'
    implementation 'org.printscript:formatter:0.2.0-SNAPSHOT'
}

test {
    // las clases de util/ son ayudantes del arnés y no tests: sin esto JUnit intenta
    // correrlas y reporta ocho initializationError que no son de la implementación
    include '**/*Test.class'
}
```

## Estado

**58 de 59.** Formatter 23/23, linter 7/7, validation 13/13, print-statement 14/14,
archivo grande 1/2.

El que falla es `InterpreterLargeFileTest.testWithCollector`, y falla por exceso de
eficiencia: corre 32.768 `println` acumulados en una lista con **8 MB de heap** y espera
que la implementación se quede sin memoria y reporte `"Java heap space"`. La nuestra
termina sin agotar la memoria, porque el pipeline es de a una sentencia y no retiene nada
entre una y la siguiente, así que la lista del arnés es lo único que crece.

El adapter igual atrapa el `OutOfMemoryError` y lo reporta como pide el TCK, para el caso
de que el arnés corra con menos memoria todavía.
