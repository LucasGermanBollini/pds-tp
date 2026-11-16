# SkillSwap

Backend del TP (Java + Hibernate/JPA + MySQL, sin Spring Boot) + la UI Swing
de la catedra, ya conectada a Controllers reales. Hitos 2, 3 y 4 completos:
[HITO3.md](HITO3.md) (persistencia JPA/MySQL) y [HITO4.md](HITO4.md)
(Controllers reales conectados al Mock UI Kit + adaptadores de
email/push/identidad, sección 7).

## Como correrlo

```bash
mvn -o test                                                            # 36 tests
java -cp target/classes ar.edu.uade.skillswap.SkillSwapApplication     # UI con datos mock (referencia de la catedra)
java -cp target/classes com.uade.skillswap.SkillSwapRealApplication    # UI con Controllers reales (requiere bootstrap-mysql.sql)
java -cp target/classes com.uade.skillswap.Main                        # demo de consola, repos en memoria (Hito 2)
java -cp target/classes com.uade.skillswap.MainJpa                     # demo de consola contra MySQL real (Hito 3)
```

(`-o` = offline; las dependencias ya estan en el repositorio local `~/.m2`.
Para MySQL real, antes correr `mysql -u root -p < scripts/bootstrap-mysql.sql`.)

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
  `NotificadorObserver` resuelve el canal preferido de cada destinatario y
  despacha la notificacion — ninguna de las dos entidades conoce que existen.
  Mismo esquema que `EmailPedidoObserver` / `FirebasePushPedidoObserver
  implements PedidoObserver` del codigo de la catedra.
- **Adapter + Simple Factory** (`infrastructure.notification`,
  `infrastructure.identidad`): `EmailNotificacionAdapter` / `PushNotificacionAdapter`
  / `NotificacionInternaAdapter` adaptan tres "SDKs externos" simulados
  (`EmailExternoSdk`, `PushExternoSdk`) a `CanalNotificacionPort`;
  `CanalNotificacionFactoryImpl` elige el canal segun el `canalPreferido` de
  cada `Perfil`. `IdentidadServiceAdapter` adapta `IdentidadExternaSdk` a
  `IdentidadProviderPort`, usado por los Controllers reales antes de delegar
  en el caso de uso. Ver [HITO4.md](HITO4.md).
- **Aplicacion (`application.usecase`)**: `PublicacionService` y
  `ContratacionService` orquestan los casos de uso contra los puertos de
  `domain.repository`, sin SQL ni UI.
- **Infraestructura**: `infrastructure.memory` (repos en memoria, Hito 2,
  todavia usados por los tests de dominio/aplicacion) e
  `infrastructure.persistence` (repos JPA reales sobre MySQL/H2, Hito 3) —
  mismo puerto de `domain.repository`, `application.usecase` no cambio una
  linea entre los dos. Ver [HITO3.md](HITO3.md).

## Decisiones que se apartan un poco del Hito 1 (y por que)

- `CanalNotificacionPort` / `CanalNotificacionFactory` se sacaron del dominio
  en el Hito 2 (al revisar el codigo real de la catedra -Clase 12 Observer:
  `EmailPedidoObserver implements PedidoObserver` directo, sin capa de
  "canal" intermedia- esa abstraccion era prematura todavia). Volvieron en
  los adaptadores (ver abajo), pero como puertos de `application.port`,
  nunca de `domain`: `NotificadorObserver` depende de ellos y por eso vive en
  `infrastructure`, no en `domain.model.observer` (ver HITO4.md, seccion
  "Adaptadores").
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

## Que falta

- Pasarela de SkillCoins simulada como Adapter (el pago se resuelve
  enteramente adentro de `ContratacionService.pagar`, moviendo saldo entre
  wallets sin gateway externo — el Mock UI Kit no tiene pantalla que
  dispare una pasarela real).
- Tests dedicados a los adaptadores nuevos (`EmailNotificacionAdapter`,
  `PushNotificacionAdapter`, `CanalNotificacionFactoryImpl`,
  `IdentidadServiceAdapter`) — hoy se ejercitan indirectamente.
- Paginacion real en la busqueda (seccion 4.3).
