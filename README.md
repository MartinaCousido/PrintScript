# PrintScript

Intérprete, formateador y analizador estático para PrintScript, un subset de TypeScript.
Trabajo práctico de Ingeniería de Software · Universidad Austral · 2026.

La arquitectura completa, con los diagramas de componentes y la responsabilidad de cada
módulo, está en **[`docs/arquitectura.md`](docs/arquitectura.md)**.

## Requisitos

**Gradle necesita un JDK 17 o superior para arrancar.** Si tu `JAVA_HOME` apunta a una
versión anterior, el build falla antes de empezar con:

```
Gradle requires JVM 17 or later to run. Your build is currently configured to use JVM 8.
```

Verificalo con:

```bash
java -version
```

**No hace falta instalar Java 25**, que es la versión con la que compila el proyecto.
Está declarada como *toolchain* en el convention plugin, y Gradle la resuelve sola la
primera vez gracias al `foojay-resolver` de `settings.gradle.kts`. Ese es justamente el
punto: la toolchain se declara en el proyecto, no en la máquina de cada uno.

### Si tenés un JDK viejo por defecto

Con SDKMAN, adoptando un JDK que ya tengas instalado (no descarga nada):

```bash
sdk install java 21-local $(/usr/libexec/java_home -v 21)
sdk default java 21-local
```

O con SDKMAN descargándolo:

```bash
sdk install java 21.0.7-tem
sdk default java 21.0.7-tem
```

Sin SDKMAN, agregando esto al final de tu `~/.zshrc`:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

## Cómo se usa

```bash
# compilar y testear todo
./gradlew build

# solo los tests
./gradlew test

# un módulo suelto: compila y testea únicamente ese
./gradlew :lexer:build

# ver deprecaciones del build
./gradlew clean build --warning-mode all
```

## Herramientas de calidad

Seis herramientas corren dentro de `./gradlew check` (que `build` incluye) y rompen el
build cuando encuentran algo. El reparto de responsabilidades: **el formateador decide
cómo se ve el código, Checkstyle decide cómo se llaman las cosas, PMD decide si la idea
es buena, CPD avisa cuando algo está escrito dos veces y JaCoCo mide qué tocaron los
tests.** Antes de agregar una regla, esa división dice dónde va.

| Herramienta | Qué hace | Sobre qué | Dónde se configura | Cuando falla |
|---|---|---|---|---|
| Spotless + google-java-format (AOSP) | escribe el formato del código | `src/**/*.java` | en ningún lado: el formateador no es configurable a propósito | `./gradlew spotlessApply` |
| ktlint (vía Spotless) | formatea los archivos de build y de texto | `*.gradle.kts`, `*.md`, `*.yml` | `.editorconfig` | `./gradlew spotlessApply` |
| Checkstyle | nombres y errores probables | `src/main` y `src/test` | `config/checkstyle/checkstyle.xml` | leer el reporte en `<modulo>/build/reports/checkstyle` y arreglar a mano |
| PMD | malas prácticas y complejidad | `src/main` y `src/test` | `config/pmd/ruleset.xml` | leer el reporte en `<modulo>/build/reports/pmd`, arreglar o justificar la supresión |
| CPD | código duplicado | `src/main` de los ocho módulos | tarea `cpdCheck` del `build.gradle.kts` raíz, umbral de 50 tokens | leer `build/reports/cpd/cpd.txt` y extraer lo repetido |
| JaCoCo | cobertura de los tests, con piso | cada módulo por separado | convention plugin `printscript.quality` | escribir el test que falta |

La cobertura es un **piso, nunca un objetivo**: mide qué líneas se ejecutaron, no si se
verificó algo, así que sirve para avisar cuando baja y no como número a perseguir. El
piso por defecto es `0.80` y sube en los módulos cuya cobertura real ya es alta; los
valores están en `buildSrc/src/main/kotlin/printscript.quality.gradle.kts`. El reporte
agregado del proyecto entero sale con `./gradlew jacocoRootReport` y queda en
`build/reports/jacoco/jacocoRootReport/html/index.html`.

CPD corre solo sobre `src/main`: las suites de test repiten estructura a propósito
(cada `TokenConstructor` y cada `StatementParser` se prueba con la misma forma) y ahí
la repetición es legibilidad, no deuda.

### Órdenes útiles

```bash
# toda la cadena: formato, estilo, PMD, CPD, tests y cobertura
./gradlew check

# una sola herramienta
./gradlew spotlessCheck
./gradlew checkstyleMain
./gradlew pmdMain
./gradlew cpdCheck
./gradlew jacocoTestCoverageVerification

# formatear sin buildear
./gradlew spotlessApply

# cobertura agregada del proyecto entero
./gradlew jacocoRootReport
```

### El hook de pre-commit

El hook vive versionado en `config/githooks/pre-commit` porque `.git/hooks/` es local a
cada clon y no viaja con el repo: un hook que no está versionado no existe para el
equipo. Por eso hay que instalarlo **una vez por clon**:

```bash
./gradlew installGitHooks
```

La tarea apunta `core.hooksPath` a `config/githooks`, así cualquier cambio al hook llega
solo al resto del equipo, sin copias.

El hook corre solo lo rápido —`spotlessCheck` y `checkstyleMain`— y **avisa y aborta**
el commit en vez de arreglarlo, para no modificar nada por debajo. Los tests, la
cobertura, PMD y CPD quedan afuera a propósito: son lo lento, y de eso se encarga el CI
sin hacerte esperar. Para un caso legítimo (un commit de emergencia, un WIP en una rama
propia) existe `git commit --no-verify`; la red que no se puede saltear es el CI.

### Dos aclaraciones

- Estas herramientas operan sobre **nuestro código Java**. No son el `formatter` ni el
  `linter` de PrintScript que pide la consigna: esos operan sobre los programas `.ps`
  del usuario y se configuran con un JSON o un YAML. El linter de PrintScript ya
  existe y está documentado más abajo, en [su propia sección](#el-linter-de-printscript).
- No usamos ktlint ni Detekt sobre el código del proyecto porque son herramientas de
  Kotlin y nuestro código es Java; sus equivalentes acá son google-java-format,
  Checkstyle y PMD. ktlint aparece únicamente para formatear los `.gradle.kts` del
  build, que sí son Kotlin.

## Módulos

Once módulos Gradle, cada uno con su propio set de dependencias y compilando por
separado. La configuración común vive en un convention plugin en `buildSrc/`, no
duplicada en cada módulo.

| Módulo | Qué esconde |
|---|---|
| `common` | Cómo viaja un dato o un error por el pipeline y cómo se ubica algo en el fuente |
| `token` | Qué es un token y qué categorías existen |
| `ast` | La forma del árbol y el contrato de recorrido |
| `runtime-api` | Cómo habla un programa en ejecución con el mundo exterior |
| `lexer` | Cómo se reconoce un token dentro del texto |
| `parser` | La gramática y cómo se construye el árbol |
| `configuration` | En qué formato vive la configuración de las herramientas (JSON, YAML) |
| `linter` | Qué convenciones se le exigen a un programa PrintScript |
| `runner` | Cómo se arma el pipeline de cada operación |
| `cli` | La experiencia de línea de comandos: argumentos, progreso, exit codes |

Faltan `semantic` y `formatter`. El grafo completo está en
[`docs/arquitectura.md`](docs/arquitectura.md).

## Cómo usar la CLI

El módulo `cli` expone las cuatro operaciones de la consigna (`execution`,
`validation`, `formatting`, `analyzing`) como subcomandos, y además abre una
interfaz interactiva (REPL) cuando se lo corre sin argumentos:

```bash
# el working directory de la tarea run es cli/, por eso la ruta es relativa a ahí
./gradlew :cli:run --args="execution src/test/resources/examples/valid-hello.ps"

# sin argumentos: interfaz interactiva para navegar y correr operaciones
./gradlew :cli:run --console=plain

./gradlew :cli:installDist
cli/build/install/printscript/bin/printscript execution ruta/al/archivo.ps
```

Referencia completa de argumentos, códigos de salida, la interfaz interactiva y
ejemplos de salida (éxito y error con fila/columna) en [`docs/cli.md`](docs/cli.md).

## Qué reconoce hoy

PrintScript 1.0 completo:

| Construcción | Ejemplo | Estado |
|---|---|---|
| Declaración | `let x: number = 12;` | ✅ |
| Declaración sin valor | `let x: number;` | ✅ |
| Asignación | `x = 5;` | ✅ |
| Println | `println(a + " " + b);` | ✅ |
| Aritmética | `+ - * /` con precedencia y paréntesis | ✅ |
| Concatenación | `string + number` en cualquier orden | ✅ |
| Strings | comillas simples y dobles | ✅ |

## El linter de PrintScript

El *static code analyzer* de la consigna. Analiza los programas del usuario, no nuestro
código Java: **no confundirlo con Checkstyle y PMD**, que están más arriba en este
mismo README.

```java
AnalysisReport reporte = new AnalyzingRunner().analyze(lineas, configuracion, "json");

reporte.violations();  // lo que incumple una convención, con su Range exacto
reporte.failures();    // los errores de sintaxis, en una lista aparte
```

Las dos listas están separadas a propósito: "esto no compila" y "esto no respeta una
convención" son cosas distintas y quien las consume decide qué hacer con cada una.
Ninguna violación corta el análisis: se reportan todas, de todas las sentencias.

### Las reglas y su configuración

Se configura con un JSON o un YAML, y cada regla se prende poniendo su clave.

| Clave | Qué exige | Valores |
|---|---|---|
| `identifier_format` | Que los identificadores tengan un formato | `camel case`, `camelCase` o `camel_case` |
| `mandatory-variable-or-literal-in-println` | Que `println` reciba solo un identificador o un literal, nunca una expresión | `true` / `false` |

```json
{
  "identifier_format": "camel case",
  "mandatory-variable-or-literal-in-println": true
}
```

Una clave que no está deja su regla apagada. Un valor **desconocido** en
`identifier_format` no apaga la regla en silencio: rompe con un mensaje que nombra el
valor recibido y los soportados.

**`snake case` no está soportado.** La consigna admite camel case o snake case y el
equipo eligió uno solo. Agregarlo es implementar `IdentifierFormatChecker` y sumar un
valor al enum `IdentifierFormat`: no cambia nada de lo que ya está escrito.

## Integración continua

Cada push a `master` y cada pull request dispara el workflow de
[`.github/workflows/ci.yml`](.github/workflows/ci.yml), que corre `./gradlew build`
con toda la cadena de calidad y publica como artifacts los reportes de test,
Checkstyle, PMD, CPD y cobertura (por módulo y agregada), también cuando el build
falla, que es cuando más sirven. El JDK del toolchain se instala en el runner para no
descargarlo en cada corrida.

## Estructura del repositorio

```
buildSrc/                 convention plugins con la configuración común del build
config/checkstyle/        reglas de nombres y errores probables
config/pmd/               ruleset de análisis estático
config/githooks/          hook de pre-commit versionado
common/  token/  ast/     vocabulario y contratos, sin lógica
runtime-api/              puertos de E/S del programa en ejecución
configuration/            lectura de la configuración de las herramientas de PrintScript
lexer/  parser/           etapas del pipeline
interpreter/  linter/
runner/                   orquesta el pipeline de punta a punta
cli/                      línea de comandos: la única que toca filesystem, System.out/err y exit codes
docs/arquitectura.md      diagramas de componentes y responsabilidades
docs/cli.md               guía de uso de la CLI
```
