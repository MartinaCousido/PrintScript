# Bugfixes de la revisión sistemática

Base: master `aec6ec6`. Java 25 se mantiene. La rama de diseño integrará esta rama
mediante merge y tendrá una PR separada.

## Defectos corregidos

- **R1 — pérdida de archivos:** execution/formatting rechazan que salida y fuente sean
  el mismo archivo (incluye aliases y symlinks). Se escribe un temporal junto al destino,
  se cierra y se reemplaza solamente si la operación terminó correctamente. Un error
  conserva el destino anterior. La salida de consola ya emitida no se revierte.
- **R2 — I/O:** la CLI traduce `UncheckedIOException` al diagnóstico y código de salida
  de uso, incluso durante el consumo de líneas y de entrada interactiva.
- **R3/R4 — configuración:** documentos/valores null se rechazan como configuración
  inválida. Los mappers JSON/YAML no cierran el stream prestado del llamador.
- **R5 — política del intérprete:** copia defensiva del conjunto de nodos admitidos.
- **R6/P3 — linter:** se incluyen reads dentro de prompts. Todos los collectors usan
  recorrido preorder iterativo, sin concatenar/copiar listas en cada nivel.
- **R7/R8 — costo cuadrático:** delimitación incremental de bloques y vistas de texto
  en el lexer. Las extensiones que implementan el contrato String siguen funcionando.
- **R12 — diagnóstico de OOM:** el adapter mantiene una reserva de 4 KiB, liberada antes
  de reportar agotamiento de heap. No garantiza recuperación de handlers arbitrarios.
- **R13 — transición del CD:** la validación de publicación usa herramientas del SHA
  del workflow y adapters del SHA publicado; evita ejecutar scripts antiguos cuando
  termina un CI iniciado antes de un cambio del CD.
- **P1 — StackOverflowError:** parser de expresiones e if/else, tipado, evaluación,
  scopes y collectors ahora usan pilas explícitas. No se impone un límite artificial
  de anidamiento. Se preservan precedencia, rangos, evaluación izquierda/derecha,
  cortocircuito ante fallos y cierre de todos los scopes ante fallos/excepciones.
- **P4 — REPL:** comillas simples/dobles y escapes de comillas/backslash para rutas;
  whitespace (incluye tab) separa el comando. Los comandos de una ruta conservan rutas
  sin comillas con espacios. Config/source admiten dos argumentos con comillas.
  Comillas sin cerrar y argumentos sobrantes se reportan sin terminar la sesión.
- **P5 — tipos de reglas:** cada consumidor valida las claves conocidas; tipos erróneos
  dejan de apagar reglas silenciosamente. Se conserva lookup genérico permisivo,
  claves ajenas (config compartida) y prioridad existente de reglas contrapuestas.

- **R14 — overflow del formatter:** suma de saltos y producto de sangría usan
  aritmética exacta. Si el tamaño excede la representación de String se devuelve
  Failure con rango, sin desbordar a negativo ni terminar la iteración por excepción.

- **R15 — calidad dependiente del checkout:** Spotless con ratchet fallaba en el
  worktree de validación (no encontraba repository). Se retira ratchet y se verifica
  toda la base, sin reducir el gate ni depender de origin/master/historia de Git.

## Evidencia

- `./gradlew build jacocoRootReport --offline --no-daemon`: **618 tests, 0 fallos,
  0 skipped**, Checkstyle, PMD, CPD, Spotless y pisos de cobertura aprobados.
- Cuatro tests activos de extremo a extremo con 10.000 paréntesis, sumas, bloques if
  y else-if. El caso de paréntesis reproducía StackOverflowError antes del cambio.
- El linter también recorre 10.000 prompts y 10.000 bloques anidados en un test activo.
- Tests adicionales: orden de efectos, cortocircuito y valores null del fold; unwind
  de scopes en fallo/excepción; conservación/aliases de archivos; recursos prestados;
  tokenizer y REPL con source/config de nombres con espacios; tipos mal configurados.
- `./tck/run-official-tck.sh 0.2.0-systematic.bugfix`: TCK oficial completo aprobado.
  No se modifican ni excluyen tests upstream. Se mantiene la adaptación ya existente
  a Java 25/heap de 5 MB, documentada en `tck/README.md`.

El CD usa el token automático del job; no requiere compartir secretos con el grupo.
La corrección de transición requiere merge para validarse en el workflow real.
Las mejoras de estado persistente, diagnósticos incrementales y dependencias
reproducibles se documentan en la PR de diseño que depende de esta rama.
