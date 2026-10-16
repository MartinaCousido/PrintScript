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

El CI ejecuta `build` y el TCK contra Maven Local en cada push y PR, sin publicar.
El CD se activa después de un CI exitoso de un push/merge a `master` o `main`, publica
el commit validado y ejecuta el TCK contra GitHub Packages en otro runner.
Los tags `v*` conservan la publicación estable y el Release, condicionado al TCK publicado.

El modo de verificación del registro es:

```sh
./tck/run-official-tck.sh 0.2.0-main.123.1 --published
```

Este modo exige `GITHUB_ACTOR` y `GITHUB_TOKEN`, omite `publishToMavenLocal` y resuelve
exclusivamente el grupo `org.printscript` desde GitHub Packages. También elimina el
repositorio Maven Local que declara upstream. Una versión local no puede ocultar un
paquete ausente o mal publicado. El job desactiva el cache compartido de Gradle.

En Actions se utiliza el token automático del job; no hay PAT, secrets manuales ni fork
personal. El CI tiene solo lectura de contenido; publicación tiene `packages: write`;
verificación tiene `packages: read`; Release tiene `contents: write`.
El TCK se ejecuta en este repositorio con las pruebas del commit upstream fijado, no
mediante una corrida remota del workflow de un fork.

Para publicar deliberadamente el working tree local, incluidos cambios sin commit:

```sh
export GITHUB_ACTOR=usuario
export GITHUB_TOKEN=token_con_write_packages
./scripts/publish-working-tree.sh 0.2.0-prueba-unica
```

El script no publica si falla `build`, cobertura o el TCK. La versión debe ser nueva: GitHub
Packages no es Maven Local y la operación sí escribe en un servicio remoto.

El job de validación usa las herramientas del SHA del workflow y los adapters del SHA
publicado en checkouts separados. Esto permite verificar un CI anterior que termine
después de actualizar CD. `PRINTSCRIPT_TCK_IMPLEMENTATION_SOURCE` selecciona el directorio
de adapters; en ejecución local el default sigue siendo `tck/implementation`.

El adapter conserva una reserva de 4 KiB para poder reportar el OOM esperado del colector.
La libera antes de llamar al handler y usa `Reference.reachabilityFence` para conservarla
durante ejecución. Las pruebas y el heap de 5 MB no se modifican por esta reserva.
