# Defensa de PrintScript en una hora — recorrido por IntelliJ IDEA

Esta guía describe el estado comprobado del último `master` auditado (`2dab86f`) más el
endurecimiento de CI/TCK de esta revisión. Está pensada para leerla en orden mientras se
navega el proyecto en IntelliJ. No reemplaza los informes V1–V3: concentra lo que conviene
poder mostrar, ejecutar y explicar oralmente.

La V3 anterior tiene 7.920 palabras: a una voz de 130 palabras por minuto dura cerca de
61 minutos; en lectura silenciosa normal, aproximadamente 32–40 minutos. Esta guía es un
repaso operativo más corto. Las cifras son estimaciones, no mediciones del reproductor.

## 1. Qué es el proyecto en treinta segundos

PrintScript es un intérprete modular para las versiones 1.0 y 1.1 de un lenguaje pequeño.
El repository es un multi-project build de Gradle 9.5.0 con trece módulos Java. La CLI es
la entrada humana; `runner` es la fachada programática; los motores son `lexer`, `parser`,
`semantic`, `interpreter`, `formatter` y `linter`; los vocabularios y puertos estables son
`common`, `token`, `ast`, `runtime-api` y `configuration`.

La frase oral útil es: “El fuente se consume como `Iterator<String>`. Para ejecutar,
`ExecutionRunner` compone `Lexer`, `TokenBuffer`, `Parser`, `SemanticAnalyzer` e
`Interpreter`. Cada etapa devuelve resultados o fallas con `Range`; el intérprete emite
por puertos de `runtime-api`. El pipeline no materializa el programa completo”.

En IntelliJ abrir primero `settings.gradle.kts`. Es la prueba de que existen exactamente
trece subproyectos. Después abrir `docs/modulos-alto-nivel.puml` o
`docs/arquitectura.md`: las flechas son dependencias, no flujo temporal.

## 2. Preparar IntelliJ antes de mostrar nada

Abrir el directorio raíz como proyecto Gradle, no cada módulo por separado. En
**Settings > Build, Execution, Deployment > Build Tools > Gradle**, usar el Wrapper. El
Wrapper está fijado en `gradle/wrapper/gradle-wrapper.properties`; por eso el equipo y CI
usan Gradle 9.5.0 aunque la máquina no tenga Gradle instalado globalmente.

El toolchain se declara en
`buildSrc/src/main/kotlin/printscript.java-library.gradle.kts`: Java 25. IntelliJ puede
tener otro JDK para arrancar, pero Gradle resuelve el compilador 25 mediante el plugin
Foojay de `settings.gradle.kts`. Ante cambios de `.gradle.kts`, pulsar **Load Gradle
Changes**.

Abrir **View > Tool Windows > Gradle**. Ahí aparecen el root y los trece módulos. Abrir
también **View > Tool Windows > Terminal**: todos los comandos de esta guía se ejecutan
desde ese terminal en la raíz. IntelliJ no cambia la semántica de `./gradlew`; solo brinda
la interfaz.

Antes de trabajar:

```bash
git status --short --branch
git pull --rebase --autostash
```

`status` evita mezclar cambios. `pull --rebase --autostash` actualiza y preserva cambios
locales temporalmente. En esta auditoría se hizo antes de modificar; la rama de trabajo
estaba detrás de `master`, así que la validación final se realizó desde un clon limpio del
último `origin/master`, sin tocar archivos locales del usuario.

## 3. Gradle que hay que poder defender

`settings.gradle.kts` define el nombre del build, el resolvedor de toolchains y los trece
módulos. `build.gradle.kts` configura lo transversal del root: coordenadas, reporte JaCoCo
agregado, CPD, hook de Git y formato de archivos de build/documentación. Cada módulo tiene
su propio `build.gradle.kts` con dependencias directas.

`buildSrc` es un build auxiliar que compila convention plugins. No es un módulo del
producto. Sus cuatro archivos clave son:

- `printscript.java-library.gradle.kts`: Java Library, Java 25, JUnit 5, repositorio Maven
  Central y publicación.
- `printscript.quality.gradle.kts`: Checkstyle, PMD, JaCoCo y umbrales.
- `printscript.format.gradle.kts`: Spotless/Google Java Format.
- `printscript.publish.gradle.kts`: `maven-publish`, `sourcesJar`, publicación Maven y
  repositorio GitHub Packages.

Una task es una unidad de trabajo con dependencias, inputs y outputs. “A depende de B”
significa que Gradle garantiza que B termine antes de A; no significa herencia de código.
El lifecycle esencial es:

- `assemble`: crea binarios y distribuciones, pero no promete tests.
- `check`: ejecuta verificaciones; en los módulos incluye tests, estilo, PMD y cobertura,
  y en el root agrega CPD.
- `build`: depende de `assemble` y `check`.
- `clean`: elimina outputs conocidos dentro de `build/`; no compila ni verifica.
- `publishToMavenLocal`: publica POM/JAR/sources en `~/.m2/repository`; no usa Internet.
- `publish`: publica en el repositorio remoto configurado, GitHub Packages, y necesita
  credenciales.

Mostrar las tasks reales desde **Gradle** o Terminal:

```bash
./gradlew tasks
./gradlew clean build jacocoRootReport --no-daemon --warning-mode all
```

El segundo comando es la comprobación completa interna. Durante esta revisión falló
primero en `:lexer:pmdMain`: `LexerFactory.merge` era un método privado agregado en el
último commit pero sin llamadas. PMD aplicó `UnusedPrivateMethod`. Se conservó únicamente
como ejemplo comentado: documenta la extensión futura sin formar parte del bytecode ni del
análisis de código muerto. Luego Spotless señaló la indentación del
comentario restante: se corrigió y el build completo quedó verde. Es un buen ejemplo de
por qué `build` vale más que “compila en mi IDE”.

## 4. Mapa de módulos y qué abrir

En el panel **Project**, seguir este orden:

| Módulo | Mostrar | Idea defendible |
|---|---|---|
| `common` | `Result`, `Failure`, `Position`, `Range`, `Version` | vocabulario de éxito/error y localización |
| `token` | `Token`, `TokenType` | salida del lexer |
| `ast` | nodos y `ASTVisitor` | representación estructural; visitor exhaustivo |
| `lexer` | `Lexer`, `LexerFactory`, `constructors/` | líneas a tokens; factory decide versión |
| `parser` | `TokenBuffer`, `Parser`, `ParserFactory` | tokens a sentencias AST |
| `semantic` | `SemanticAnalyzer`, `SymbolTable`, `ExpressionTyper` | tipos, símbolos, const y scopes |
| `runtime-api` | `RuntimePorts` y tres interfaces | inversión de dependencias para I/O |
| `interpreter` | `Interpreter`, `Executor`, `ExpressionEvaluator`, `Environment` | AST validado a efectos/valores |
| `configuration` | `ConfigurationReader`, mappers JSON/YAML | config externa a reglas |
| `formatter` | `Formatter`, `Gaps` | transforma espacios/saltos desde tokens |
| `linter` | `LinterFactory`, reglas y collectors | políticas configurables sobre AST |
| `runner` | cuatro runners y `StatementStreams` | composición del pipeline |
| `cli` | `Cli`, `RootCommand`, comandos | filesystem, consola, progreso y exit codes |

La modularidad sigue el criterio de razones de cambio: el lexer puede cambiar cómo
reconoce tokens sin obligar al interpreter a conocer texto; `runtime-api` permite que el
interpreter pida entrada, entorno y salida sin depender de consola o sistema operativo.
Las dependencias exactas se comprueban en cada `build.gradle.kts`, no por el dibujo.

## 5. Recorrido de un programa

Tomar conceptualmente:

```text
const name: string = readInput("Name:");
if (true) { println("Hello " + name); }
```

En `cli/Cli.main` Picocli delega en `RootCommand` y `ExecutionCommand`.
`SourceFileProcessor` abre el archivo como flujo y `ProgressReportingIterator` envuelve
las líneas. `ExecutionRunner.run` recibe líneas, `RuntimePorts` y `Version.V11`.

`StatementStreams.validatedStatementStream` arma la cadena. `LexerFactory` aporta las
keywords de 1.1 (`const`, `if`, booleanos, `readInput`, `readEnv`) y el lexer entrega
`Token`. `TokenBuffer` conserva solo los tokens necesarios para una sentencia o bloque.
`ParserFactory` arma parsers compatibles con 1.1; el resultado contiene
`ConstDeclaration`, `ReadInput`, `IfStatement`, `Block`, `PrintlnStatement` y
`BinaryExpression`.

`SemanticAnalyzer` recorre el AST: registra `name` como constante `string`, comprueba el
booleano del `if`, los tipos de concatenación y scopes. `InterpreterFactory` entrega el
visitor permitido para 1.1. `Executor` maneja sentencias y `ExpressionEvaluator` valores;
`Environment` conserva variables y frames de bloques. `readInput` llama al puerto de
entrada, el prompt al puerto de salida, y `println` al mismo puerto. En CLI esos puertos
son consola; en tests son dobles; en el TCK son adapters.

Una falla léxica, sintáctica o semántica viaja como `Failure` con `Range`. La CLI la
presenta mediante `FailureReporter` y devuelve un `ExitCode`; no hay que afirmar que todo
error de dominio se lanza como excepción.

## 6. Versiones 1.0 y 1.1

`common/Version.java` enumera `V10` y `V11`. El contenido de la versión se distribuye en
cuatro factories: `LexerFactory`, `ParserFactory`, `SemanticFactory` e
`InterpreterFactory`. El motor recibe componentes ya elegidos; no debería llenarse de
`if (version == ...)`.

1.0 cubre `let`, `number`, `string`, asignación, aritmética, concatenación y `println`.
1.1 suma `const`, `boolean`, `if/else`, bloques, `readInput` y `readEnv`, además de reglas
de formatter/linter. Esto coincide con las consignas descargadas. La CLI acepta `-v 1.0`
y `-v 1.1`, default 1.0; verificarlo en `LanguageVersion` y `docs/cli.md`.

## 7. Testing interno

El convention plugin agrega JUnit Jupiter. Los tests viven en cada
`<módulo>/src/test/java`. No hay un source set separado llamado `integrationTest`; la
diferencia es conceptual y por nombre:

- Unitarios: prueban clases locales, por ejemplo `TokenTest`, `EnvironmentTest`,
  `ExpressionBuilderTest`.
- Integración/end-to-end de librería: `runner/*EndToEndTest`, recorren varias etapas.
- End-to-end de CLI: `cli/*EndToEndTest`, ejercitan comandos, archivos, salida y códigos.
- Compatibilidad externa: los 59 tests del TCK oficial, en otro repository.

Para ejecutar desde IntelliJ, usar el triángulo verde junto a una clase o método. Para
reproducir CI, preferir el panel Gradle o Terminal; el runner propio de IntelliJ puede
usar classpath/opciones distintos.

```bash
./gradlew test
./gradlew :parser:test
./gradlew :parser:test --tests 'org.printscript.parser.IfParserTest'
./gradlew :runner:test --tests '*ConsignaExamplesEndToEndTest'
```

Para agregar un test, crear la clase en el mismo package bajo `src/test/java`, usar
`@Test` de JUnit 5, ejecutar primero el método y luego el `build` completo. Los umbrales
JaCoCo se configuran en `printscript.quality.gradle.kts`; que todos los tests pasen no
garantiza por sí solo la cobertura mínima.

## 8. Quality gates probados

Las pruebas de mutación manual documentadas en V3 confirmaron que los gates fallan cuando
deben: formato incorrecto activa Spotless; nombre inválido activa Checkstyle;
`System.out.println` activa PMD; duplicar una clase activa CPD; elevar cobertura a 100%
activa JaCoCo; cambiar una assertion activa JUnit. Las mutaciones se hicieron en clones
aislados y se restauraron.

En el estado actual el último commit introdujo código muerto y PMD lo detectó. Por eso la
respuesta correcta a “¿qué controla CI?” es: compilación, JUnit, Spotless, Checkstyle,
PMD, JaCoCo y CPD mediante el grafo de `build`; luego TCK mediante un step explícito. Los
reportes se suben aun si falla el build gracias a `if: always()`.

## 9. TCK oficial, ahora reproducible

El TCK no está incorporado como módulo porque pertenece a la cátedra y define su propio
contrato (`PrintScriptFactory`). Nuestro repository conserva únicamente adapters en
`tck/implementation/`. `CustomImplementationFactory` implementa las tres fachadas que
pide el kit; `PrintScriptAdapters` traduce `InputStream`, versión, errores y puertos.

Desde Terminal de IntelliJ:

```bash
./tck/run-official-tck.sh 0.2.0-SNAPSHOT
```

El script publica los trece módulos del working tree en Maven Local, clona el TCK en
`build/official-tck`, fija su commit oficial, copia los adapters y ejecuta exactamente 59
tests. `tck/tck.init.gradle` declara las seis dependencias que consume el adapter, JDK 25
y heap 5 MB. Fijar el commit evita que un cambio externo convierta un push idéntico en un
resultado distinto; actualizarlo debe ser una decisión revisada.

El caso difícil compara dos consumidores sobre 32.768 `println`: `PrintCounter` no
retiene salidas y debe terminar; `PrintCollector` las acumula y debe reportar `Java heap
space`. Con JDK 25 y máximo 7 MB el collector entraba y quedaban 58/59; con 5 MB el
collector agota memoria y el contador sigue pasando. No se detecta el nombre del test ni
se inserta un OOM artificial en producción: se fija la frontera ambiental que la prueba
pretende observar. Resultado comprobado: 59/59.

## 10. CI: qué ocurre en cada push

Abrir `.github/workflows/ci.yml`. Se dispara en todo `push` y en `pull_request`. El job
corre en `ubuntu-latest`, hace checkout con historia completa, instala Temurin 25 y prepara
Gradle. Después:

1. ejecuta `build jacocoRootReport` sobre los trece módulos;
2. crea `PACKAGE_VERSION=0.2.0-ci.<run>.<sha-corto>`;
3. ejecuta el TCK contra artefactos Maven Local de ese mismo checkout;
4. si el evento es `push`, publica esa versión exacta en GitHub Packages;
5. siempre intenta subir reportes de tests, Checkstyle, PMD, CPD y JaCoCo.

`permissions: packages: write` habilita el `GITHUB_TOKEN`. En PR se omite publicación:
los PR de forks no deben recibir credenciales de escritura. “Pasa el último commit” se
garantiza porque checkout, build, publicación local, TCK y publicación remota ocurren en
el mismo job y workspace; si un paso falla, los pasos siguientes normales no corren.

No decir que CI hace deploy. Publica snapshots consumibles: eso es entrega de paquetes.
No instala una aplicación en un servidor.

## 11. CD, packaging y GitHub Packages

Abrir `.github/workflows/cd.yml`. Se dispara solo al pushear un tag `v*`. Deriva la
versión quitando `v`, corre `build`, corre el TCK, genera `:cli:distZip` y `distTar`,
publica los trece módulos Maven y crea un GitHub Release con las distribuciones CLI.

Las palabras no son sinónimos:

- Build: compilar y verificar (`build`).
- Package: producir JAR/ZIP/TAR (`jar`, `distZip`, `distTar`).
- Publish: copiar paquetes a un repository (`publishToMavenLocal` o `publish`).
- Deploy: instalar/activar en un ambiente de ejecución. No existe deployment en este repo.

Maven coordinates: group `org.printscript`, artifactId igual al nombre de módulo,
version pasada por `-PprintscriptVersion`. Maven Local queda bajo
`~/.m2/repository/org/printscript/<módulo>/<versión>/`. GitHub Packages usa
`https://maven.pkg.github.com/MartinaCousido/PrintScript`.

Publicar remotamente el working tree desde IntelliJ:

```bash
export GITHUB_ACTOR=mi_usuario
export GITHUB_TOKEN=token_con_write_packages
./scripts/publish-working-tree.sh 0.2.0-prueba-miusuario-1
```

La versión debe ser única. El script ejecuta build/cobertura, TCK y recién entonces
`publish`. Incluye modificaciones sin commit porque Gradle compila el filesystem actual.
Eso es útil para una prueba deliberada, pero no brinda trazabilidad de Git por sí sola;
los snapshots de CI sí incluyen SHA. Alternativamente las credenciales pueden estar en
`~/.gradle/gradle.properties` como `gpr.user` y `gpr.key`, aunque el script exige las
variables para hacer explícita la operación remota.

## 12. Consignas: cómo se cumplen

Primera parte: pide intérprete, formatter, analizador estático, CLI, posiciones completas,
modularidad, tests y procesamiento de fuentes mayores a memoria. En código se ve en los
seis motores, `Range`, comandos CLI, tests end-to-end y el pipeline iterador. El límite de
streaming es al bloque/sentencia corriente, no necesariamente O(1) respecto del bloque.

Segunda parte: pide selección de versión y rechazo de features incompatibles; `const`,
booleanos, `if/else`, `readInput`, `readEnv`; puertos; indentación configurable; brace en
misma línea; regla de linter para `readInput`. Se ubica en las cuatro factories, nodos AST,
`runtime-api`, `Conversions`, configuración del formatter y
`ReadInputArgumentRule`. Los tests `Version11*`, `Read*`, `If*`, `Const*` y
`ConsignaExamplesEndToEndTest` son evidencia directa.

Lo que no debe afirmarse sin matiz: no existe gramática ANTLR; el parser es manual. No hay
Dockerfile ni Docker Compose. No hay deployment. El TCK no se descarga como dependencia:
se clona y consume nuestros artefactos Maven.

## 13. Doce factores y el mensaje de Discord

El artículo compartido conecta 12-factor app con Docker, pero el mensaje aclara que Docker
todavía no fue tratado. En este repo no hay Docker: no inventar una implementación.

Sí hay conexiones parciales defendibles. Config vive fuera del código de lenguaje mediante
JSON/YAML (`configuration`) y credenciales por entorno/Gradle properties
(`printscript.publish.gradle.kts`). Dependencias y build son declarativos y reproducibles
con Gradle Wrapper. Entrada, entorno y salida se separan mediante puertos
(`runtime-api`), lo que desacopla backing services. CI produce logs en consola y artefactos.
Las builds y releases usan versiones trazables.

Pero PrintScript es una CLI/librería, no un servicio web stateless: factores como port
binding, concurrencia por procesos, disposability o admin processes no tienen una
implementación equivalente clara. Respuesta modelo: “El artículo muestra que Docker puede
operacionalizar varios factores; nuestro proyecto ya aplica configuración externa,
dependencias declaradas y puertos, pero no contiene Docker ni un deployment, así que no
afirmaría cumplimiento integral”.

## 14. Cambios típicos frente al profesor

Agregar dependencia: abrir el `build.gradle.kts` del módulo consumidor y elegir `api` si
forma parte de su API pública o `implementation` si es interna; recargar Gradle y correr
el build.

Cambiar Java: abrir `printscript.java-library.gradle.kts`, modificar toolchain, revisar
CI `setup-java`, compatibilidad del Wrapper y TCK; es un cambio coordinado, no una línea.

Agregar sintaxis: empezar por `TokenType` si hace falta, luego `LexerFactory`, AST,
`ParserFactory`, `SemanticFactory`, `InterpreterFactory`; el visitor exhaustivo mostrará
implementaciones pendientes. Agregar unit tests y un end-to-end en `runner`.

Agregar regla del linter: implementar `LintRule`, collector/visitor si hace falta, exponer
configuración en `LinterConfiguration`/`LinterFactory`, agregar mapper key y tests.

Cambiar CI: editar `.github/workflows/ci.yml`; distinguir step (secuencia dentro de un job)
de job (runner independiente). Ejecutar localmente el comando Gradle, revisar YAML y diff;
las actions/permissions solo se prueban realmente en GitHub.

## 15. Preguntas rápidas y respuestas hablables

**¿Build y assemble son iguales?** No. `build` agrega verificaciones de `check`; `assemble`
solo produce artefactos.

**¿Por qué Wrapper?** Fija Gradle 9.5 y hace reproducible el build sin instalación global.

**¿Dónde está el entry point?** `cli/src/main/java/org/printscript/cli/Cli.java`, método
`main`; `cli/build.gradle.kts` lo configura para `application`.

**¿Quién compone el intérprete?** `ExecutionRunner` y `StatementStreams`; los motores no
dependen horizontalmente entre sí.

**¿Cómo se valida sin ejecutar?** `ValidationRunner` consume el stream validado y junta
fallas; no recibe `RuntimePorts`.

**¿Por qué formatter trabaja con tokens?** Porque el AST pierde whitespace y el formatter
debe cambiar solo huecos configurados, incluso si el programa no tipa.

**¿Maven Local publica a Internet?** No. Es un repository de la máquina bajo `~/.m2`.

**¿Qué prueba el TCK?** El contrato externo común de interpreter, formatter y linter,
incluidas versiones y streaming; no reemplaza unit tests internos.

**¿CI publica?** En push sí publica un snapshot con SHA después de build y TCK; en PR corre
ambas suites pero no publica. Los tags disparan el workflow CD de release.

**¿Hay deployment?** No. Hay packaging, publicación Maven y GitHub Releases.

**¿Qué estaba roto?** El último commit dejó un método privado sin uso en `LexerFactory`;
PMD frenaba `build`. Se conservó como ejemplo comentado, no como método compilado. El TCK tenía además una frontera de heap no
reproducible en JDK 25; el arnés automatizado fija 5 MB y pasa 59/59.

## 16. Machete final de IntelliJ

1. **Project** → `settings.gradle.kts`: trece módulos.
2. **Project** → `buildSrc`: Java, calidad, formato, publicación.
3. **Gradle** → root → `build`: package + verification.
4. **Terminal** → `./gradlew clean build jacocoRootReport`.
5. **Project** → `runner/StatementStreams`: composición.
6. **Project** → factories: variación 1.0/1.1.
7. **Project** → `runtime-api`: puertos.
8. **Terminal** → `./tck/run-official-tck.sh 0.2.0-SNAPSHOT`: 59/59.
9. **Project** → `.github/workflows/ci.yml`: push/PR, build, TCK, snapshot.
10. **Project** → `.github/workflows/cd.yml`: tag, TCK, packages, CLI release.
11. **Terminal** → `publish-working-tree.sh <versión única>` solo con credenciales y con
    intención real de escribir en GitHub Packages.
12. Si preguntan Docker: **NO ENCONTRADO EN EL REPOSITORY**; conectar solo conceptos
    parciales de 12-factor, sin simular infraestructura inexistente.

La tesis final para defender: la fortaleza del proyecto no es una clase individual, sino
que las decisiones están separadas, los bordes son explícitos, el pipeline es streaming,
las versiones se ensamblan en factories y ahora el mismo commit debe superar calidad,
tests internos y contrato externo antes de convertirse en paquete.
