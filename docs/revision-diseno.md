# Diseño y reproducibilidad de la revisión sistemática

## Base y separación

La rama `design/systematic-quality` contiene un merge explícito de
`fix/systematic-correctness` ([PR #19](https://github.com/ingsis-ua/ingsis-interpreter/pull/19)).
Esta PR se apila sobre esa rama para mostrar solamente diseño. Al integrar #19,
se cambia la base de esta PR a master. Java 25 se mantiene.

## Cambios

- **P2 — estado persistente:** frames enlazados con entrada/salida O(1); mapas AVL
  inmutables con actualización O(log n). Lectura recorre scopes hasta encontrar la
  sombra más cercana. Reemplazo exterior reconstruye solo los frames intermedios.
  Los snapshots conservan sus valores. El algoritmo balanceado usa recursión O(log n)
  por tamaño del mapa, independiente del anidamiento del programa.
- **P2 — diagnósticos incrementales:** overloads de execution, validation, formatting
  y analyzing reciben consumers y devuelven boolean de éxito. Las APIs anteriores
  conservan sus listas/reportes. CLI y TCK usan los nuevos overloads; errores y
  violaciones se entregan por sentencia, sin acumular el programa entero.
- **R9/R10/R11 — contratos y legibilidad:** patrones anónimos de Java 25, retiro de
  supresión obsoleta, documentación de propiedad de streams/writers, grafo/memoria y
  APIs reales, retiro de dependencia API de common que configuration no usa.
- **P6 — reproducibilidad:** Gradle lockfiles con modo estricto en raíz/módulos/buildSrc;
  SHA-256 para dependencias y metadata; checksum oficial del wrapper; todas las Actions
  fijadas a SHA completo; Dependabot semanal para Gradle/Actions.
- Inventario por archivo con hashes y reporte de R1–R15/P1–P6, incluyendo criterios,
  compatibilidad, decisiones y limitaciones. No quedan hallazgos registrados pendientes.

Los collectors compartidos y recorridos iterativos de AST están en #19 porque
resuelven el desbordamiento del JVM y permiten los tests de anidamiento, no solo estilo.

## Compatibilidad y límites

Las firmas antiguas siguen disponibles y materializan diagnósticos como antes.
Los consumers pueden ejecutar efectos y sus excepciones se propagan. La CLI ahora
intercala errores y violaciones en orden de sentencias. La memoria depende de la
línea/sentencia actual, símbolos vivos y consumidores externos; un bloque enorme no
se transforma en memoria constante. Configuration lookup permisivo, prioridades de
reglas y valores de variables sin inicializar conservan contratos existentes.

Los checksums iniciales fijan los bytes obtenidos de repositorios/cache configurados;
no autentican al autor ni prueban ausencia de vulnerabilidades. Actualizaciones de
locks/checksums se revisan; Dependabot no hace auto-merge. El mantenimiento exacto y
las fuentes oficiales están en `docs/revision-sistematica.md`.

## Validación

- Build completo: tests, Checkstyle, PMD, CPD, Spotless y pisos de cobertura.
- 624 tests locales, cero fallos y cero skipped (618 bugfixes + 6 diseño).
- Persistencia: 20.000 escrituras ordenadas/inversas/aleatorias comparadas con TreeMap,
  colisiones de hash, snapshots y asignación exterior con 10.000 scopes.
- Streaming: 10.000 errores entregados antes de leer la siguiente línea, sin listas.
- Cuatro regresiones activas de programas de profundidad/longitud 10.000 heredadas de #19.
- TCK oficial completo: 59/59, sin modificar/excluir tests, Java 25 y heap existente de 5 MB.
- Verificación estricta de dependencias activa durante build y publicación local del TCK.

CI corre en pushes y PRs. CD publica después de CI exitoso en main/master y valida
los paquetes descargados del registro en otro runner. Usa el token automático de
GitHub Actions: no se agregan secrets manuales, PAT ni fork personal.
