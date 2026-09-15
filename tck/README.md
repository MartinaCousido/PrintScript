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

## Comando reproducible

```sh
./tck/run-official-tck.sh 0.2.0-SNAPSHOT
```

El script publica los trece módulos del working tree en Maven Local, clona el TCK oficial
en `build/official-tck`, fija el commit `510028a8246f6fd607a3195394310f509d9047a2`,
copia los adapters y ejecuta sus 59 casos. `tck/tck.init.gradle` agrega las dependencias,
JDK 25 y la frontera de heap sin modificar el repositorio externo.

## Ajustes aplicados al TCK

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
    minHeapSize = '5m'
    maxHeapSize = '5m'
}
```

## Estado

**59 de 59**, verificado con JDK 25, Gradle 9.5.0 y el commit fijado del TCK.

`InterpreterLargeFileTest.testWithCollector` depende deliberadamente de una frontera de
memoria: acumula 32.768 salidas y espera `"Java heap space"`; `testWithCounter` procesa la
misma entrada sin retenerla y debe terminar. El upstream declara 5 MB iniciales y 7 MB
máximos, pero con JDK 25 el collector entra en 7 MB. Fijar máximo e inicial en los 5 MB ya
declarados por el TCK conserva el propósito: el collector agota memoria, el counter no.

## Automatización

`.github/workflows/ci.yml` ejecuta primero todo el `build`, luego este TCK y, solo en
eventos `push`, publica en GitHub Packages una versión única trazable al SHA. Los pull
requests ejecutan build y TCK, pero no publican porque un PR de fork no recibe credenciales
de escritura. `.github/workflows/cd.yml` también exige el TCK antes de publicar un tag.

Para publicar deliberadamente el working tree local, incluidos cambios sin commit:

```sh
export GITHUB_ACTOR=usuario
export GITHUB_TOKEN=token_con_write_packages
./scripts/publish-working-tree.sh 0.2.0-prueba-unica
```

El script no publica si falla `build`, cobertura o el TCK. La versión debe ser nueva: GitHub
Packages no es Maven Local y la operación sí escribe en un servicio remoto.
