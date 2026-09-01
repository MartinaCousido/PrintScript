# CLI de PrintScript

El módulo `cli` (`org.printscript.cli.Cli`, comando `printscript`) es el único punto de
entrada ejecutable del sistema y el único módulo autorizado a tocar el filesystem,
`System.out`/`System.err` y los códigos de salida — el resto son librerías puras. Para
el porqué de esa separación, ver [`docs/arquitectura.md`, sección 5 y 9](arquitectura.md).

## Cómo compilar y correr

Sin generar una distribución, usando el toolchain de Gradle (no hace falta tener Java 25
instalado):

```bash
# el working directory de la tarea run es cli/, por eso la ruta es relativa a ahí
./gradlew :cli:run --args="execution src/test/resources/examples/valid-hello.ps"
```

Generando una distribución instalable (requiere un `java` de PATH compatible con el
toolchain del proyecto, hoy Java 25; si tu `java` por defecto es más viejo, usá
`./gradlew :cli:run` de arriba):

```bash
./gradlew :cli:installDist
cli/build/install/printscript/bin/printscript execution ruta/al/archivo.ps
```

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
| `validation <archivo>` | `validate` | Valida sin ejecutar |
| `formatting <archivo>` | `format`, `fmt` | No implementado todavía |
| `analyzing <archivo>` | `analyze`, `lint` | No implementado todavía |
| `pwd` | | Muestra el directorio actual |
| `ls [directorio]` | `dir` | Lista el contenido (directorios primero, con `/`) |
| `cd <directorio>` | | Cambia de directorio (acepta `..` y rutas absolutas) |
| `help` | `?` | Muestra la ayuda con todos los comandos |
| `exit` | `quit` | Sale de la interfaz (también con Ctrl-D) |

Las rutas de archivo son relativas al directorio actual del REPL (el que muestra
`pwd`), no al working directory del proceso. Cada operación usa exactamente el mismo
camino de lectura, progreso y reporte de errores que su subcomando equivalente
(`SourceFileProcessor`), así que el resultado es idéntico al de la forma no interactiva.

Bajo `./gradlew :cli:run` conviene pasar `--console=plain` para que Gradle no pise el
prompt con su propia salida de progreso; el binario de `installDist` no lo necesita.

## Operaciones

| Operación | Alias | Qué hace |
|---|---|---|
| `execution` | `exec` | Ejecuta el archivo: corre lexer → parser → intérprete e imprime la salida del programa por stdout |
| `validation` | `validate` | Valida sintaxis (y semántica, cuando exista el módulo `semantic`) sin ejecutar nada |
| `formatting` | `format`, `fmt` | **No implementado todavía** — el módulo `formatter` no existe |
| `analyzing` | `analyze`, `lint` | **No implementado todavía** — los módulos `linter`/`semantic` no existen |

Las cuatro quedan expuestas como choice cerrado porque así lo pide la consigna, aunque
solo `execution` y `validation` tengan backend hoy: `formatting`/`analyzing` terminan
con un mensaje explícito de "no implementado" y exit code `3`, nunca con un resultado
simulado.

## Argumentos

```
printscript [-h] [-v=<version>] COMANDO ARCHIVO [-c=<config>]
```

| Argumento | Aplica a | Descripción |
|---|---|---|
| `ARCHIVO` | las 4 operaciones | Posicional, requerido. Debe existir y ser un archivo regular. |
| `-v`, `--version` | global | Opcional, default `1.0`. Es la versión del **lenguaje** PrintScript a interpretar, no la versión de la CLI. Hoy solo se acepta `1.0`; cualquier otro valor falla con exit code `2`. |
| `-c`, `--config` | `formatting`, `analyzing` | Archivo de configuración YAML/JSON de reglas. Se acepta y valida su forma, pero como esas operaciones no están implementadas todavía, no se usa. |
| `-h`, `--help` | global | Muestra la ayuda de picocli para el comando (o subcomando) actual. |

## Códigos de salida

| Código | Significado |
|---|---|
| `0` | Éxito: sin errores reportados |
| `1` | El archivo `.ps` tiene errores de validación o ejecución (sintácticos o semánticos) |
| `2` | Error de uso: argumentos inválidos, versión no soportada, archivo inexistente |
| `3` | La operación pedida (`formatting`/`analyzing`) todavía no está implementada |

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

Operación no implementada:

```bash
$ printscript formatting valid-hello.ps
Formatting no está implementado todavía: el módulo formatter no existe (ver docs/arquitectura.md).
$ echo $?
3
```

## Progreso durante el parsing

Mientras se leen las líneas del archivo, `cli` escribe el porcentaje de avance por
`stderr` (`Progreso: NN%`), estimado por bytes consumidos contra `Files.size` del
archivo — sin releer el archivo para calcularlo. La salida real del programa
ejecutado va exclusivamente por `stdout`, así que redirigir la salida
(`printscript execution archivo.ps > salida.txt`) captura solo lo que el programa
imprime, nunca el progreso ni los mensajes de error.
