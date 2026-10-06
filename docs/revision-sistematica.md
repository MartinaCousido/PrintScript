# Revisión sistemática del proyecto

Base: `master` `aec6ec6c26fae25d875a7d52e2e38e59b626db2e` después de integrar #17 y #18. Fecha: 2026-10-06.

## Alcance y evidencia

Se leyeron los contratos y fuentes de los 13 módulos (136 archivos Java de
producción en esta rama), los dos adapters del TCK y la configuración de build/calidad.
Se inventariaron los escenarios de 101 archivos Java de test, se revisaron en detalle
los tests de los contratos modificados y se ejecutó la suite completa. Eso no equivale a
una auditoría manual línea por línea de todos los tests ni a una auditoría de vulnerabilidades.
El [inventario por archivo](revision-inventario.tsv) registra el nivel de revisión y hash
de cada archivo, incluidos fixtures, documentación y wrapper. Los archivos locales no
versionados del usuario no forman parte de esta revisión.

Criterios: responsabilidad de módulos, dependencias de APIs (`api`/`implementation`),
extensión por versión/regla, propiedad de recursos, estado mutable, errores y recuperación,
streaming/costos, legibilidad, tests de contratos y publicación reproducible.

## Hallazgos corregidos

| ID | Prioridad | Evidencia y efecto | Corrección |
|---|---|---|---|
| R1 | Alta | `ExecutionOutput`/`Formatting` abrían la salida truncando el fuente si era el mismo archivo. Una operación fallida podía destruir un destino previo. | Rechazo de mismo archivo/enlaces; helper `FileOutputs` escribe temporal, cierra y reemplaza solo con éxito. Tests end to end y de fallo de escritura. |
| R2 | Media | `BufferedReader.lines()` e input/writers producen `UncheckedIOException`, que escapaba del manejo de CLI. | Captura en fronteras de I/O y exit code explícito; tests de consumo de fuente y de entrada interactiva. |
| R3 | Media | JSON `null` o claves con valor `null` terminaban en NPE de `Map.copyOf`, fuera del diagnóstico de configuración. | Rechazo explícito como configuración inválida, con test. |
| R4 | Media | Jackson cerraba por defecto el stream del llamador. | Mappers dejan abierto el stream prestado; contrato y test para JSON/YAML. |
| R5 | Media | Cambiar el `Set` pasado al intérprete cambiaba la política de nodos admitidos. | Copia defensiva, verificada mutando el conjunto original. |
| R6 | Media | `ReadInputCollector` encontraba la llamada externa y omitía llamadas dentro de su prompt. | Traversal del prompt; test del orden de ambas llamadas. |
| R7 | Media | `BlockAwareDelimiter` recorría todo el prefijo por cada token: delimitación O(n²) en bloques grandes. | Scanner por sentencia con profundidad incremental O(n); API anterior preservada. Test prohíbe releer tokens previos. |
| R8 | Media | Lexer copiaba `substring(columnIndex)` en cada token: costo cuadrático para líneas largas con muchos tokens. | Vistas `CharSequence` sin copia para constructores incorporados, puente compatible para implementaciones String existentes. Tests de vistas y selección existentes. |
| R9 | Baja | Comentario y supresión de PMD de `SimpleArgument` correspondían al cambio a Java 21. | Patrones anónimos de Java 25 y retiro de la supresión. |
| R10 | Baja | Documentación afirmaba que nadie dependía de los motores, que no había acumuladores y que execution heredaba del comando abstracto. También se hacía flush dos veces. | Documentación ajustada al grafo y memoria reales; propiedad del Writer y flush único. |
| R11 | Baja | `configuration` exponía `common` como dependencia API sin usar sus clases. | Dependencia eliminada; build/TCK verifican el grafo. |

Los archivos de salida conservan el destino ante errores del programa; los mensajes
emitidos por consola y otros efectos externos ya realizados no se deshacen. El movimiento
es atómico cuando el filesystem lo soporta, con fallback estándar. Reemplaza la entrada
de destino: un enlace de salida diferente del fuente se reemplaza por el archivo generado.

Se corrigió también R12: el adapter conserva 4 KiB durante ejecución para que, al liberar
esa reserva después de un OOM, el handler pueda guardar un diagnóstico pequeño. Se mantiene
alcanzable mediante Reference.reachabilityFence. La primera prueba detectó un segundo OOM
al crecer la lista de errores del handler; con la reserva, el TCK volvió a 59/59. No garantiza
recuperación de cualquier handler arbitrario ni reemplaza pruebas o cambia el heap.

R13 corresponde al CD: una corrida antigua de CI puede terminar después de actualizar el
workflow. La verificación ahora usa herramientas de github.workflow_sha y adapters del SHA
publicado, en checkouts separados. La corrida 37409761168 reprodujo el fallo usando un script
antiguo sin --published. La corrección remota queda pendiente del merge de esta rama. La selección de
github.workflow_sha sigue el [contrato oficial del contexto GitHub](https://docs.github.com/en/actions/reference/workflows-and-actions/contexts).

## Evaluación por módulo

| Módulo | Contrato y responsabilidad | Resultado |
|---|---|---|
| common | Result/Range/Version y estado de scopes | Result preserva errores; Success(null) es válido para Void. ScopedValues mantiene persistencia/sombra; P2 elimina copias completas al escribir. |
| token | Vocabulario genérico, sin palabra clave por enum | Tipos/rangos/texto no nulos; extensión de keywords queda en factory. |
| ast | Records y Visitor exhaustivo | Bloques copian listas; Visitor exhaustivo y adapters son decisiones explícitas. Agregar nodos requiere revisar todos los visitors; no justifica introducir jerarquías adicionales ahora. |
| runtime-api | Puertos de input/output/env independientes | Sin dependencias de proyecto. RuntimePorts valida referencias; las implementaciones controlan efectos e I/O. |
| lexer | Clasificación configurable, selección de mayor longitud | R8 elimina copias de sufijos para builtins. Extensiones String siguen funcionando con fallback de copia. |
| parser | Gramática y límites de sentencias separados por interfaces | R7 evita rescaneos. Parser mantiene recuperación por sentencia; P1 resuelve anidamiento por pilas explícitas. |
| semantic | Tipos y scopes sin depender de lexer/interpreter | Recuperación y declaración de símbolo tras initializer inválido están fijadas por tests; no se cambió esa política. |
| interpreter | Efectos por puertos y evaluación separada | R5 fija inmutabilidad de configuración. Valor sin inicializar null y representación de números son comportamientos existentes; no se redefinieron como refactor. |
| configuration | Formatos por mapper y lookup tipado | R3/R4/R11. No hay Jackson en consumidores. P5 valida esquemas de consumidores, manteniendo lookup permisivo. |
| formatter | Reescritura de tokens sin depender de AST/parser | Preserva parentesización/comillas; reglas con prioridad explícita. Recibe código no parseable deliberadamente; rangos válidos y ordenados son precondición. |
| linter | Reglas independientes y configuración tipada | R6 cubre anidamiento omitido. P3 centraliza traversal iterativo. |
| runner | Construcción del pipeline y consumo de resultados | Dependencias de implementación de motores y APIs expuestas son coherentes. P2 agrega consumers incrementales; las APIs de listas siguen disponibles. |
| cli | Entrada/salida, argumentos y códigos de proceso | R1/R2 protegen recursos/archivos. Helpers separan navegación, fuente, destinos y reportes; P4 agrega tokenizer con comillas. |

El grafo de producción es acíclico. Los motores consumen vocabulario/puertos y no dependen
entre sí. Las factories son el punto de composición por versión; reglas y mappers son
extensibles sin editar el motor. No se introdujeron interfaces para toda clase concreta:
la inversión de dependencias ya existe donde cambia el proveedor (puertos, parsers,
mappers, reglas), y una interfaz adicional sin alternativa real aumenta el mantenimiento.

## Segunda pasada: todos los hallazgos abiertos aplicados

| ID | Estado | Implementación y contrato |
|---|---|---|
| P1 | Corregido en bugfixes | Frames de parsing y continuaciones de if/else explícitos; folds de expresiones y traversal de scopes sin recursión por profundidad del fuente. Cuatro regresiones activas con 10.000 niveles/operadores; sin límite artificial ni skip. |
| P2 | Corregido en diseño | ScopedValues usa frames enlazados O(1) para entrada/salida y mapas AVL persistentes O(log n) para escrituras en un frame. Actualizar un scope exterior copia solo los frames intermedios, preservando snapshots. Runner agrega consumidores incrementales; CLI/TCK los usan. Fachadas antiguas que devuelven listas permanecen compatibles y materializan sus resultados deliberadamente. |
| P3 | Corregido en bugfixes | NodeCollector comparte traversal preorder con pila explícita y un acumulador. Incluye nested prompts, declaraciones/assignments y ambas ramas; mantiene orden/rangos. También elimina StackOverflow de linter sobre AST profundo. |
| P4 | Corregido en bugfixes | Tokenizer del REPL con comillas simples/dobles, escapes y whitespace. Source/config con espacios, tab como separador, diagnóstico de comillas inválidas y recuperación de sesión. Se conserva ruta simple sin comillas con espacios. |
| P5 | Corregido en bugfixes | validateTypes separa lookup permisivo de validación de claves conocidas. Formatter/Linter rechazan tipos incorrectos; mantienen claves ajenas y prioridades existentes, verificadas por tests. |
| P6 | Corregido en diseño | Locks estrictos para raíz, módulos y buildSrc; verificación SHA-256 de artefactos/metadata; checksum oficial de distribución Gradle; Actions fijadas a commits completos verificados por API; Dependabot semanal. |

**R14 (última pasada):** el formatter sumaba saltos y multiplicaba sangría con overflow
silencioso de int. La rama de bugfixes usa aritmética exacta y devuelve Failure con rango
cuando supera la representación admitida, con tests que no necesitan asignar enormes Strings.

**R15:** se retira ratchet de Spotless: el formatter de Java verifica todos los archivos y no depende de historia/referencias de Git ni falla en worktrees.

No queda un hallazgo R1–R15/P1–P6 abierto. Esto no certifica ausencia de otros defectos:
la revisión incluye contratos, errores, complejidad, resources, extensión y build; no es
una auditoría de vulnerabilidades ni redefine comportamientos del lenguaje fijados por tests.

### Reproducibilidad y mantenimiento

Los [locks](https://docs.gradle.org/9.5.0/userguide/dependency_locking.html) fijan la
resolución; los [checksums](https://docs.gradle.org/9.5.0/userguide/dependency_verification.html)
rechazan modificaciones de los artefactos aceptados. La metadata inicial se generó con
Gradle desde los repositorios configurados/cache actual; no equivale a autenticar al autor
ni a auditar CVEs. Se deben revisar los cambios de checksums y origen al actualizar:

```bash
./gradlew build jacocoRootReport --write-locks --write-verification-metadata sha256 --no-daemon
./gradlew build jacocoRootReport --no-daemon
./tck/run-official-tck.sh 0.2.0-review.local
```

La segunda ejecución verifica los archivos sin regenerarlos. Dependabot propone cambios,
pero no se configura auto-merge. Las Actions se fijan a commits completos conforme al
[contrato de seguridad de GitHub](https://docs.github.com/en/actions/reference/security/secure-use).

### División de PRs

- Bugfixes: `fix/systematic-correctness` sobre master, [PR #19](https://github.com/ingsis-ua/ingsis-interpreter/pull/19).
- Diseño: `design/systematic-quality`, contiene un merge explícito de bugfixes. PR
  inicialmente apilada sobre la rama de bugfixes para mostrar solo diseño; después de
  integrar #19 con merge commit (sin squash/rebase) se cambia la base a master. La historia compartida evita resolver nuevamente
  los cambios de bugfixes al integrar diseño (sin cambios concurrentes sobre las mismas áreas).

## CI/CD y credenciales

#17 y #18 quedaron mergeadas con checks exitosos. El CD
[37409384697](https://github.com/ingsis-ua/ingsis-interpreter/actions/runs/37409384697)
terminó exitosamente: publicación en GitHub Packages y TCK en runner nuevo descargando
esa versión. No usa un fork personal ni PAT/secrets manuales; cada job recibe el token
automático y permisos limitados. No hay valores de credenciales en este informe.

Se conservan los 59 tests upstream fijados y la configuración preexistente de Java 25 y
heap de 5 MB. Ese límite de heap es una adaptación ya documentada en master; no se afirma
que la configuración de ejecución sea idéntica a los 7 MB máximos declarados upstream.

## Validación de esta rama

- `./gradlew build jacocoRootReport --offline --no-daemon`: build, tests, Checkstyle,
  PMD, CPD, Spotless y pisos de cobertura: **624 tests, cero fallos y cero skipped**.
- `./tck/run-official-tck.sh 0.2.0-systematic.design`: **59/59** después de corregir R12.
- Tests nuevos comprueban conservación de archivos, limpieza temporal, fallos de I/O,
  propiedad de streams, política inmutable, traversal anidado y lectura sin rescaneos/copias.
- Cuatro regresiones activas de extremo a extremo ejercitan 10.000 niveles/operadores; las nuevas pruebas de persistencia comparan 20.000 escrituras con un TreeMap y mantienen snapshots a través de 10.000 scopes.
- Tests de streaming comprueban 10.000 diagnósticos entregados antes de solicitar la siguiente línea.
- Build aprobado con locks y checksums estrictos, sin regenerar metadata.
- Java 25, tests upstream y heap preexistente se conservan.
