# SkillSwap — Hito 2 (Núcleo)

Dominio puro y casos de uso de la Aplicación, sin JPA ni UI todavía (eso llega
en los Hitos 3 y 4). Repositorios en memoria para poder demostrar el flujo
completo ya mismo.

## Como correrlo

```bash
mvn -o test                                   # 29 tests, dominio + aplicacion
mvn -o compile
java -cp target/classes com.uade.skillswap.Main   # demo de consola
```

(`-o` = offline; junit-jupiter 6.0.3, maven-compiler-plugin 3.15.0 y
maven-surefire-plugin 3.5.6 ya estan en el repositorio local `~/.m2`.)

## Que esta implementado

- **Dominio (`domain.model`)**: `Perfil`, `Publicador`, `FranjaDisponibilidad`,
  `Publicacion`, `Contratacion`, `HistorialEstado`, `Calificacion`,
  `MovimientoCreditos`, `WalletSkillCoins`, `Notificacion`, value object
  `SkillCoins` (evita primitive obsession, Clase 3).
- **State** (`domain.model.state`): ciclo de vida completo de `Publicacion`
  (5 estados) y `Contratacion` (7 estados), con clase base que rechaza
  transiciones invalidas — mismo esquema que `EstadoPedido`/`EstadoPedidoBase`
  del material de la catedra.
- **Composite**: `ElementoCatalogo` / `Habilidad` (Leaf) / `CategoriaHabilidad`
  (Composite) para el catalogo de habilidades.
- **Decorator**: `Cotizable` / `PrecioPublicacion` / `PromocionDecorator` +
  `DescuentoPorcentual` / `DescuentoMontoFijo`, acumulables y sin precio
  negativo.
- **Strategy**: `RecomendacionStrategy` (afinidad por habilidades /
  popularidad reciente) y `PoliticaCancelacionStrategy` (Estandar / Flexible).
- **Simple Factory**: `RecomendacionStrategyFactory` resuelve la estrategia
  por nombre sin que el caso de uso tenga un if/switch propio.
- **Observer**: `Publicacion` y `Contratacion` implementan `SujetoObservable`;
  `RegistradorHistorialObserver` persiste el historial inmutable y
  `NotificadorObserver` genera la notificacion interna — ninguna de las dos
  entidades conoce que existen. Mismo esquema que `EmailPedidoObserver` /
  `FirebasePushPedidoObserver implements PedidoObserver` del codigo de la
  catedra: el canal real NO se adapta con una capa de "factory" propia del
  dominio, sino con un Observer concreto por canal, en Infraestructura.
- **Aplicacion (`application.usecase`)**: `PublicacionService` y
  `ContratacionService` orquestan los casos de uso contra los puertos de
  `domain.repository`, sin SQL ni UI.
- **Infraestructura provisoria (`infrastructure.memory`)**: repositorios en
  memoria que implementan los puertos del dominio. Se reemplazan por
  JPA/Hibernate en el Hito 3 sin tocar `application.usecase`.

## Decisiones que se apartan un poco del Hito 1 (y por que)

- Se sacaron `CanalNotificacionPort` / `CanalNotificacionFactory` del dominio.
  Al revisar el codigo real de la catedra (Clase 12 Observer:
  `EmailPedidoObserver implements PedidoObserver` directo, sin una capa de
  "canal" intermedia) se vio que esa abstraccion era prematura para este
  hito: el Adapter hacia email/push se resuelve con un Observer concreto por
  canal en Infraestructura (Hito 3), elegido por una Simple Factory que
  recien tiene sentido cuando existen esos adaptadores reales. Por ahora
  `NotificadorObserver` genera directamente la `Notificacion` interna.
- Los puertos hacia servicios externos (como `ProcesadorPago` en el ejemplo
  de pagos de la Clase 10 Strategy+Adapter) viven en `application.port`, no
  en `domain.repository` ni en `domain.model`. Todavia no agregamos ninguno
  porque el pago con SkillCoins en este hito es puramente interno (mover
  saldo entre dos `WalletSkillCoins` existentes, sin gateway externo
  real); si en el Hito 3 se decide simular una pasarela externa para
  SkillCoins, el puerto va ahi, siguiendo ese mismo ejemplo.
- `Publicacion.precio` se simplifico a `precioBase: SkillCoins` liso; la
  cadena de `Cotizable`/`PromocionDecorator` se construye en el momento de
  pagar (en el caso de uso), no se guarda permanentemente en la publicacion.
- El "cupo" de la seccion 4.2 se modela en `FranjaDisponibilidad` (cada franja
  tiene su propio cupo), no como un campo aparte en `Publicacion`.

## Que falta para el Hito 3

- Persistencia real con JPA/Hibernate + MySQL y migraciones (Flyway/Liquibase).
- Adaptadores concretos de identidad, email/push y pasarela de SkillCoins.
- Optimistic locking a nivel de base de datos sobre `FranjaDisponibilidad`.
