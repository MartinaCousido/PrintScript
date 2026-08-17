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

## Módulos

Siete módulos Gradle, cada uno con su propio set de dependencias y compilando por
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
| `interpreter` | Qué efecto produce cada nodo al ejecutarse |

Faltan `semantic`, `config`, `formatter`, `linter`, `runner` y `cli`. El grafo completo
está en [`docs/arquitectura.md`](docs/arquitectura.md).

## Qué reconoce hoy

PrintScript 1.0, con una excepción marcada:

| Construcción | Ejemplo | Estado |
|---|---|---|
| Declaración | `let x: number = 12;` | ✅ |
| Declaración sin valor | `let x: number;` | ✅ |
| Asignación | `x = 5;` | el nodo del AST y su ejecución existen; falta el parseo |
| Println | `println(a + " " + b);` | ✅ |
| Aritmética | `+ - * /` con precedencia y paréntesis | ✅ |
| Concatenación | `string + number` en cualquier orden | ✅ |
| Strings | comillas simples y dobles | ✅ |

## Integración continua

Cada push a `master` y cada pull request dispara el workflow de
[`.github/workflows/ci.yml`](.github/workflows/ci.yml), que compila los siete módulos y
corre todos los tests. El JDK del toolchain se instala en el runner para no descargarlo
en cada corrida.

## Estructura del repositorio

```
buildSrc/                 convention plugin con la configuración común del build
common/  token/  ast/     vocabulario y contratos, sin lógica
runtime-api/              puertos de E/S del programa en ejecución
lexer/  parser/           etapas del pipeline
interpreter/
docs/arquitectura.md      diagramas de componentes y responsabilidades
```
