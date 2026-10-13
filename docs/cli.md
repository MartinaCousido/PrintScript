# CLI de PrintScript

El módulo `cli` (`org.printscript.cli.Cli`, comando `printscript`) es el único punto de
entrada ejecutable del sistema y el único módulo autorizado a tocar el filesystem,
`System.out`/`System.err` y los códigos de salida — el resto son librerías puras. Para
el porqué de esa separación, ver [`docs/arquitectura.md`, sección 5 y 9](arquitectura.md).

## Cómo compilar y correr

Sin generar una distribución, usando el toolchain de Gradle (no hace falta tener Java 25
instalado):

```bash
# las rutas parten de la raíz del repositorio, también al correr con Gradle
./gradlew :cli:run --args="execution cli/src/test/resources/examples/valid-hello.ps"
```

Generando una distribución instalable (requiere un `java` de PATH compatible con el
toolchain del proyecto, hoy Java 25; si tu `java` por defecto es más viejo, usá
`./gradlew :cli:run` de arriba):

```bash
./gradlew :cli:installDist
cli/build/install/printscript/bin/printscript execution ruta/al/archivo.ps
```

La tarea `:cli:run` usa la raíz del repositorio como directorio de trabajo. El
binario instalado usa el directorio desde el que lo invocás. En ambos casos podés
usar rutas absolutas; en la sesión interactiva `pwd` muestra la base de las rutas.

## Interfaz interactiva (REPL)

Si se corre la CLI **sin argumentos**, en vez de fallar por falta de operación se abre
una interfaz interactiva dentro de la terminal, donde se navega el filesystem y se
corren todas las operaciones sin volver a invocar el proceso cada vez:

```bash
./gradlew :cli:run --console=plain    # o el binario: printscript (sin argumentos)
```

```
PrintScript — interfaz interactiva. Escribí 'help' para ver los comandos.
Directorio actual: /home/…/programas
printscript:programas> ls
subcarpeta/
saludo.ps
printscript:programas> exec saludo.ps
Hola, Tute
printscript:programas> validate roto.ps
error: Expected ':' after the variable name (línea 1, columna 7 a línea 1, columna 12)
printscript:programas> exit
```

Comandos disponibles:

| Comando | Alias | Qué hace |
|---|---|---|
| `execution <archivo>` | `exec`, `run` | Ejecuta el archivo e imprime su salida |
| `save <archivo>` | `guardar` | Ejecuta y guarda la salida de los `println` en un `.txt` junto al fuente (`ejemplo.ps` → `ejemplo.out.txt`), además de mostrarla |
| `validation <archivo>` | `validate` | Valida sin ejecutar |
| `formatting <archivo> [config]` | `format`, `fmt` | Formatea el archivo con las reglas del config e imprime el resultado (segundo token opcional) |
| `analyzing <archivo> [config]` | `analyze`, `lint` | Lintea el archivo con las reglas del config (segundo token opcional) |
| `pwd` | | Muestra el directorio actual |
| `ls [directorio]` | `dir` | Lista el contenido (directorios primero, con `/`) |
| `cd <directorio>` | | Cambia de directorio (acepta `..` y rutas absolutas) |
| `version [1.0\|1.1]` | `v` | Muestra o cambia la versión del lenguaje de la sesión (inicial: `1.0`) |
| `help` | `?` | Muestra la ayuda con todos los comandos |
| `exit` | `quit` | Sale de la interfaz (también con Ctrl-D) |

Las rutas de archivo son relativas al directorio actual del REPL (el que muestra
`pwd`), no al working directory del proceso. Cada operación usa exactamente el mismo
camino de lectura, progreso y reporte de errores que su subcomando equivalente
(`SourceFileProcessor`), así que el resultado es idéntico al de la forma no interactiva.

Las rutas con espacios se agrupan con comillas simples o dobles:

```text
format "mi programa.ps" "mis reglas.json"
analyze 'mi programa.ps' 'mis reglas.json'
exec "mi programa.ps"
cd "mis programas"
version 1.1
```

`format` y `analyze` aceptan uno o dos argumentos; un argumento extra o comillas
sin cerrar producen un mensaje de uso y la sesión sigue abierta. Los comandos que
reciben una sola ruta también conservan la forma anterior sin comillas (`exec mi
programa.ps`). Las comillas agrupan rutas; no hay expansión de variables ni escapes
como en una shell del sistema.

Bajo `./gradlew :cli:run` conviene pasar `--console=plain` para que Gradle no pise el
prompt con su propia salida de progreso; el binario de `installDist` no lo necesita.

## Operaciones

| Operación | Alias | Qué hace |
|---|---|---|
| `execution` | `exec` | Ejecuta el archivo: corre lexer → parser → intérprete e imprime la salida del programa por stdout |
| `validation` | `validate` | Valida sintaxis y semántica sin ejecutar nada |
| `formatting` | `format`, `fmt` | Formatea el archivo con el módulo `formatter`, aplicando las reglas del `--config`; escribe por stdout o, con `--output`, a un archivo |
| `analyzing` | `analyze`, `lint` | Analiza estáticamente con el módulo `linter`, aplicando las reglas del `--config` (sin config, avisa que no hay reglas de lint activas) |

Las cuatro operaciones tienen backend hoy. `validation` valida sintaxis **y semántica**:
la cadena pasa por el módulo `semantic`, así que un `let x: number = "hola";` se reporta sin
ejecutar nada.

### `execution`: guardar la salida (`--output` / `save`)

`execution -o salida.txt` (o el comando `save` del REPL) escribe la salida de los
`println` del programa a un archivo, además de mostrarla por stdout. El REPL deriva el
nombre del archivo del fuente (`ejemplo.ps` → `ejemplo.out.txt`, en el mismo directorio);
el subcomando toma la ruta explícita del `--output`. Solo se guarda lo que el programa
imprime, nunca el progreso ni los errores. El mensaje de confirmación va por `stderr`,
para mantener `stdout` limpio al redirigirlo. Si el fuente no existe, no se crea el `.txt`.

En `execution` y `formatting`, `--output` debe señalar un archivo distinto del fuente.
Si es el mismo, incluso a través de `..`, un enlace simbólico o un enlace duro, la CLI
rechaza la operación con código `2` **antes de abrir o truncar la salida**. El fuente
queda intacto. Un archivo de salida distinto que ya existe se reemplaza.

### `formatting`: reglas del formatter

El formatter **reescribe el fuente**: ajusta el espacio que alguna regla activa nombra y
deja el resto tal como estaba. Una clave ausente no cae en un valor por defecto, significa
**no tocar ese espacio**. Sin `--config` no se aplica ninguna regla de formato y la CLI muestra un aviso por
`stderr`; también avisa si el archivo de configuración no activa ninguna regla. Por defecto el resultado va por stdout; `--output archivo` lo escribe a un
archivo (nunca pisa el fuente). Reglas disponibles:

| Clave | Qué controla | Valores |
|---|---|---|
| `enforce-spacing-before-colon-in-declaration` | Espacio antes del `:` en declaraciones | `true` |
| `enforce-spacing-after-colon-in-declaration` | Espacio después del `:` en declaraciones | `true` |
| `enforce-spacing-around-equals` | Un espacio de cada lado del `=` | `true` |
| `enforce-no-spacing-around-equals` | Ningún espacio alrededor del `=` | `true` |
| `mandatory-space-surrounding-operations` | Un espacio alrededor de `+ - * /` | `true` |
| `mandatory-single-space-separation` | Exactamente un espacio entre todos los tokens de una línea, salvo antes del `;` | `true` |
| `mandatory-line-break-after-statement` | Una sentencia por línea | `true` |
| `line-breaks-after-println` | Líneas en blanco **después** de cada `println` | entero ≥ 0 |
| `if-brace-same-line` | La `{` del `if` en la misma línea | `true` |
| `if-brace-below-line` | La `{` del `if` en la línea de abajo | `true` |
| `indent-inside-if` | Sangría de cada nivel dentro de un bloque | entero ≥ 0 |

Las claves son las que nombra la consigna. Cada una vale `true` (o un entero) para
prenderse; ponerla en `false` es lo mismo que no ponerla. `enforce-spacing-around-equals` y
`enforce-no-spacing-around-equals` configuran el mismo espacio en sentidos opuestos, igual
que el par de `if-brace-*`.

Ninguna regla cubre el espacio entre una keyword y lo que sigue (`let   x`): para eso está
`mandatory-single-space-separation`, que normaliza todos los espacios de la línea.

```bash
$ cat feo.ps
let x:number=(2+3)*4;
println(x);
$ cat reglas.json
{ "enforce-spacing-after-colon-in-declaration": true, "enforce-spacing-around-equals": true }
$ printscript formatting feo.ps -c reglas.json
let x: number = (2+3)*4;
println(x);
$ echo $?
0
```

Los paréntesis y el `*` quedan pegados porque ninguna regla activa habla de ellos:
agregando `mandatory-space-surrounding-operations` saldría `(2 + 3) * 4`.


### `analyzing`: reglas del linter

El linter aplica las reglas que declara el archivo `--config` (JSON o YAML). Una clave
ausente deja su regla apagada; sin `--config`, no hay ninguna regla activa: se siguen reportando los errores de sintaxis, pero no
las violaciones de convenciones. La CLI muestra un aviso por `stderr` cuando no hay
reglas activas, incluso si se pasó un config vacío o con todas las reglas apagadas. Reglas disponibles:

| Clave | Qué exige | Valores |
|---|---|---|
| `identifier_format` | El formato de los identificadores | `camel case` o `snake case` (cada uno acepta también `camelCase`/`camel_case` y `snakeCase`/`snake_case`) |
| `mandatory-variable-or-literal-in-println` | Que `println` reciba solo un identificador o un literal | `true` / `false` |
| `mandatory-variable-or-literal-in-readInput` | Lo mismo para el argumento de `readInput` | `true` / `false` |

```bash
$ printscript analyzing programa.ps -c reglas.json
lint: El identificador 'last_name' debe estar escrito en camel case (línea 1, columna 5 a línea 1, columna 13)
$ echo $?
1
```

Las violaciones se reportan por `stderr` con el prefijo `lint:` y el rango exacto de lo
que incumplen; los errores de sintaxis se reportan como en `validation`. El exit code es
`1` si hay al menos una violación o un error, `0` si el archivo está limpio.

## Argumentos

```
printscript [-h] [-v=<version>] COMANDO ARCHIVO [-c=<config>] [-o=<salida>]
```

| Argumento | Aplica a | Descripción |
|---|---|---|
| `ARCHIVO` | las 4 operaciones | Posicional, requerido. Debe existir y ser un archivo regular. |
| `-v`, `--version` | global | Opcional, default `1.0`. Es la versión del **lenguaje** PrintScript a interpretar, no la versión de la CLI. Se aceptan `1.0` y `1.1`; cualquier otro valor falla con exit code `2`. |
| `-c`, `--config` | `formatting`, `analyzing` | Archivo de configuración YAML/JSON de reglas, que se lee y se aplica (ver arriba). El formato se detecta por la extensión (`.json`/`.yaml`/`.yml`); una extensión desconocida o un archivo inexistente fallan con exit code `2`. |
| `-o`, `--output` | `execution`, `formatting` | Archivo de salida. En `execution` guarda la salida del programa (además de mostrarla); en `formatting` escribe el código formateado ahí en vez de por stdout. |
| `-h`, `--help` | global | Muestra la ayuda de picocli para el comando (o subcomando) actual. |

## Códigos de salida

| Código | Significado |
|---|---|
| `0` | Éxito: sin errores ni violaciones reportadas |
| `1` | El archivo `.ps` tiene errores de validación/ejecución (sintácticos o semánticos) o el análisis encontró violaciones |
| `2` | Error de uso: argumentos inválidos, versión no soportada, archivo o configuración inexistente o mal formada |
| `3` | Operación pedida pero no implementada (reservado: hoy las cuatro operaciones tienen backend) |

## Ejemplos

Programa válido (`valid-hello.ps`):

```
let name: string = "Joe";
let lastName: string = "Doe";
println(name + " " + lastName);
```

```bash
$ printscript execution valid-hello.ps
Joe Doe
$ echo $?
0
```

Programa con un error de sintaxis (`syntax-error-missing-colon.ps`):

```
let x number = 1;
println("despues");
```

```bash
$ printscript execution syntax-error-missing-colon.ps
despues
error: Expected ':' after the variable name (línea 1, columna 7 a línea 1, columna 12)
$ echo $?
1
```

La segunda sentencia se ejecuta igual (el pipeline no corta al primer error, ver
[`docs/arquitectura.md`, sección 8](arquitectura.md)); `validation` sobre el mismo
archivo reporta el mismo error, con exit code `1`, pero sin ejecutar nada (sin
`despues` en la salida).

Guardar la salida a un archivo:

```bash
$ printscript execution valid-hello.ps -o salida.txt
Joe Doe
Salida guardada en salida.txt
$ cat salida.txt
Joe Doe
```

## Progreso durante el parsing

Mientras se leen las líneas del archivo, `cli` escribe el porcentaje de avance por
`stderr` (`Progreso: NN%`), estimado por bytes consumidos contra `Files.size` del
archivo — sin releer el archivo para calcularlo. La salida real del programa
ejecutado va exclusivamente por `stdout`, así que redirigir la salida
(`printscript execution archivo.ps > salida.txt`) captura solo lo que el programa
imprime, nunca el progreso ni los mensajes de error.
