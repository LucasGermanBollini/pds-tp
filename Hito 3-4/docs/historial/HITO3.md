# Hito 3 — Infraestructura (JPA/MySQL)

Persistencia real con Hibernate/JPA + MySQL, migraciones versionadas con
Flyway, repositorios JPA que reemplazan a los de memoria del Hito 2 sin
tocar `application.usecase`, y optimistic locking sobre el cupo de las
franjas. Verificado con 34 tests (29 del Hito 2 + 5 nuevos de persistencia),
todos en verde, contra H2 en modo MySQL.

## Cómo correrlo

```bash
mvn -o test                         # 34 tests (incluye persistencia sobre H2)
```

Para probarlo contra MySQL real (no hace falta para los tests, que corren
sobre H2):

```bash
mysql -u root -p < scripts/bootstrap-mysql.sql   # una vez, con tu root
mvn -o compile
java -cp "target/classes;<classpath>" com.uade.skillswap.MainJpa
```

`scripts/bootstrap-mysql.sql` crea un usuario `skillswap` dedicado — la app
nunca usa tu root real, ni yo lo necesito para nada de esto.

## Qué se agregó

- **Mapeo JPA en las entidades del Hito 2** (mismo compromiso que usa la
  cátedra: las anotaciones de persistencia viven directo en `domain.model`,
  no en clases espejo separadas).
- **State persistido como texto + reconstrucción**: `Publicacion.estado` y
  `Contratacion.estado` son ahora una columna `String` (`estadoNombre`) más
  un campo `@Transient` que se reconstruye en `@PostLoad` vía
  `EstadoPublicacionFactory`/`EstadoContratacionFactory` — el mismo esquema
  que `Pedido.estado` + `EstadoPedidoFactory` de la cátedra (Clase 11/13).
- **Composite persistido**: `ElementoCatalogo` sigue siendo una interfaz de
  dominio; se agregó `ElementoCatalogoBase` (abstracta, `@Entity`,
  `@Inheritance(SINGLE_TABLE)`) de la que heredan `Habilidad` (Leaf) y
  `CategoriaHabilidad` (Composite) — igual que `Vendible implements
  ItemVendible` en el ejemplo de Composite de la cátedra.
- **SkillCoins como `@Embeddable`**: value object embebido en cada entidad
  que lo usa (`precioBase`, `saldo`, `monto`, `montoPagado`), no una tabla
  propia.
- **Optimistic locking real**: `FranjaDisponibilidad` tiene `@Version`.
  `OptimisticLockingTest` prueba que dos transacciones que leen la misma
  fila no pueden guardar ambas su cambio de cupo (seccion 8/15).
- **`UnitOfWork`** (`application.port`, mismo lugar que `ProcesadorPago` del
  ejemplo de Strategy+Adapter de la cátedra): límite transaccional real para
  `solicitar`/`aceptar`/`pagar`/`cancelar`/`calificar`, que tocan varias
  entidades y necesitan que todo se guarde junto o nada (seccion 8). Los
  repositorios en memoria del Hito 2 usan un `NoOpUnitOfWork`; JPA usa
  `JpaUnitOfWork`, que abre un `EntityManager`, lo publica en `JpaContext`
  (ThreadLocal) para que los repositorios lo reutilicen, y hace commit/
  rollback de todo junto.
- **Repositorios JPA** (`infrastructure.persistence.repository`), mismo
  esquema que `SimpleJpaRepository` de la cátedra pero transaction-aware:
  si hay una transacción ambiente (abierta por `JpaUnitOfWork`), la
  reutilizan; si no, abren y cierran la suya (uso standalone).
- **Flyway**: `db/migration/V1__init.sql` (esquema completo, generado a
  partir del mapeo real de las entidades para garantizar que coincide
  exactamente — no escrito a mano y después "corregido a ojo") y
  `V2__seed_demo_data.sql` (datos reproducibles). `hibernate.hbm2ddl.auto
  =validate` en `persistence.xml`: Hibernate nunca crea ni modifica tablas,
  solo valida que coincidan (seccion 8: "no depender de ddl-auto=create").
- **N+1**: `JpaPublicacionRepository.buscarPublicadasConFranjas()` usa
  `LEFT JOIN FETCH` para el listado masivo. Las colecciones que el dominio
  muta justo después de un `buscarPorId()` (`franjas`, `etiquetas`,
  `habilidades`) se dejaron en `FetchType.EAGER` a propósito — ver la nota
  en el código de `Publicacion`/`Perfil` sobre por qué LAZY ahí rompía con
  `LazyInitializationException` en cuanto la sesión de carga se cerraba.

## Bugs reales que aparecieron recién con persistencia real (y no en Hito 2)

Vale la pena dejarlos anotados porque son el tipo de cosa que in-memory
nunca iba a mostrar:

1. **Los observers no sobreviven una recarga.** `SujetoObservable.observadores`
   es `@Transient` (igual que en `Pedido` de la cátedra). En memoria, cargar
   una Contratacion siempre devolvía el mismo objeto Java, así que esto
   nunca fallaba. Con JPA, `aceptar()`/`pagar()`/etc. cargan una instancia
   nueva en cada llamada → sin observers → el historial no se escribía. Se
   corrigió re-conectando los observers dentro de `obtener(...)` en ambos
   Services, no solo al crear la entidad.
2. **`FranjaDisponibilidad` no tenía `equals()`/`hashCode()`.** Sin eso,
   `publicacion.getFranjas().contains(franja)` compara por referencia, y
   una franja recién leída de la base nunca es "la misma" que la instancia
   original en memoria. Se agregó `equals`/`hashCode` por id, igual que
   `Habilidad`/`CategoriaHabilidad`.
3. **`merge()` de una entidad que referencia un objeto transient sin
   cascade falla.** Pasar a `Contratacion` una `FranjaDisponibilidad` que
   nunca pasó por un repositorio (aunque ya exista una fila para su id,
   guardada indirectamente por cascada desde `Publicacion`) tira
   `TransientPropertyValueException`. La solución fue tomar la franja desde
   el objeto `Publicacion` ya persistido (`publicacion.getFranjas().get(0)`),
   no desde la variable local original — así es como lo haría un caller real.

## Qué falta para el Hito 4

- Adaptadores concretos de identidad, email/push (Observer concretos, como
  `EmailPedidoObserver` de la cátedra) y pasarela de SkillCoins simulada.
- Controllers conectados a la interfaz Swing/JavaFX provista por la cátedra.
