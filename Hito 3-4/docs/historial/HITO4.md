# Hito 4 — Interfaz conectada a Controllers reales

`MockSkillSwapControllers` reemplazado por Controllers reales que delegan en
`application.usecase`. Mismo `SkillSwapView` del Mock UI Kit, sin tocar una
pantalla (seccion 10). Ver [HITO4_PLAN.md](HITO4_PLAN.md) para el análisis
previo de la interfaz `UiControllers`; esto documenta lo que efectivamente
se construyó a partir de ese plan.

## Cómo correrlo

```bash
mvn -o test                                          # 36 tests (sin cambios de Hito 3)
java -cp target/classes ar.edu.uade.skillswap.SkillSwapApplication   # UI con datos mock (referencia de la catedra)
java -cp target/classes com.uade.skillswap.SkillSwapRealApplication  # UI con Controllers reales (requiere MySQL, ver README)
```

`SkillSwapRealApplication` siembra un perfil `user-mateo` (misma persona que
el Mock, con wallet y una publicación de ejemplo) la primera vez que corre
contra una base vacía — así la UI no arranca en blanco.

## Qué se agregó

- **`application.usecase`**: `PublicacionService` ganó `buscar(...)` (texto,
  categoría, modalidad, precio máximo, orden), `listarCategorias()`,
  `listarPropias(autorUserId)`, `crearBorradorSimple(...)` (find-or-create
  de categoría por nombre + franja por defecto) y `buscarPorId(...)`.
  `ContratacionService` ganó `buscarPorId(...)` y `listarPorParticipante(...)`.
  Dos casos de uso nuevos: `WalletService` (saldo, movimientos, crédito
  demo) y `NotificacionService` (listar, marcar leída/s). Y `PerfilService`
  nuevo, que arma `PerfilResumen` (Perfil + conteo de contrataciones
  COMPLETADA del usuario).
- **`domain.repository`**: queries nuevas — `PublicacionRepository.buscarPorAutor`,
  `ContratacionRepository.buscarPorParticipante`, `MovimientoCreditosRepository.buscarPorWallet`,
  `ElementoCatalogoRepository.buscarCategoriaPorNombre` / `listarCategorias` —
  implementadas tanto en memoria como en JPA (JPQL).
- **`presentation.controller`** (nuevo): `BusquedaUiController`,
  `PublicacionUiController`, `ContratacionUiController`, `WalletUiController`,
  `NotificacionUiController`, `PerfilUiController` implementan las
  sub-interfaces de `UiControllers` del kit; `SkillSwapRealControllers` las
  agrupa; `UiMappers` traduce entre las entidades de dominio y los records
  de `Models`; `AppFactory` arma todo el grafo de dependencias a mano (sin
  Spring Boot) a partir de un set de repositorios.
- **`SkillSwapRealApplication`**: mismo `SkillSwapView`, Controllers reales
  sobre JPA/MySQL, con el seed de `user-mateo` descripto arriba.

## Decisiones de traducción UI ↔ dominio

- **`cambiarEstado`/`avanzar` genéricos → acciones específicas**: el
  Controller mira el estado actual (`getEstadoNombre()`) y llama al método
  de dominio que corresponde. Para `avanzar()` en estado `ACEPTADA`, eso
  significa llamar `pagar(...)` con `new PrecioPublicacion(precioBase)` — la
  UI no sabe que "avanzar" ahí implica mover SkillCoins, es el Controller
  quien lo decide.
- **`solicitar` sin franja**: el Controller toma la primera
  `FranjaDisponibilidad` de la publicación con cupo disponible. Si no hay
  ninguna, lanza `IllegalStateException` (no hay UI para elegir franja en
  este kit).
- **`cancelar` sin motivo**: usa el default que ya quedó en `Contratacion.cancelar()`.
- **`categoria` como String**: `crearBorradorSimple` hace *find-or-create*
  contra `ElementoCatalogoRepository` — el Composite de categorías sigue
  existiendo en el dominio, la UI solo ve el nombre.
- **Franja por defecto al crear un borrador**: como el kit no pide franjas
  al crear una publicación, `crearBorradorSimple` crea una (en 7 días, cupo
  5) para que `validarRequisitosDePublicacion()` no bloquee publicar.
- **`Perfil.rol`**: campo nuevo (`rolDescripcion`), no existía en el dominio
  de los Hitos 2/3.
- **`Movimiento.saldo`**: `MovimientoCreditos` ahora guarda `saldoPosterior`
  (el saldo de la wallet justo después del movimiento, estilo libro mayor)
  para poder listarlo sin recalcular nada.
- **`destacada`**: siempre `false` — no hay un concepto de "publicación
  destacada" en el dominio: en el Mock era data curada a mano.

## Bug real que apareció al probar el flujo end-to-end (no en los tests unitarios)

Armé un harness que ejercita los Controllers reales con el `SkillSwapView`
de verdad (captura de pantalla, ver abajo) en vez de solo unit tests, y
encontró algo que ningún test anterior detectaba: **las notificaciones y el
historial se duplicaban** en cuanto una misma `Contratacion` pasaba por dos
o más transiciones seguidas (aceptar → pagar → iniciar → completar) sobre
repositorios en memoria.

Causa: `Publicacion`/`ContratacionService.obtener(...)` re-conecta los
observers en cada llamada (son `@Transient`, hace falta — ver HITO3.md).
Pero `RegistradorHistorialObserver`/`NotificadorObserver` no tenían
`equals()`, así que `Contratacion.agregarObservador()` (que evita duplicados
con `!observadores.contains(...)`) nunca detectaba que ya había uno del
mismo tipo adjunto, y los iba acumulando dentro de la misma instancia en
memoria (con JPA no pasaba, porque cada carga es un objeto nuevo). Se
corrigió agregando `equals()`/`hashCode()` por clase a ambos observers: solo
puede haber un registrador de historial y un notificador adjuntos a la vez,
sin importar cuántas veces se intente re-wirearlos.

Los 36 tests existentes no lo habían detectado porque ninguno asertaba
sobre la *cantidad* de notificaciones/historial generadas, solo sobre su
contenido.

## Captura de referencia

Pantalla "Inicio" con Controllers reales: `2300 SkillCoins` (saldo real tras
un pago de prueba), `2 NUEVAS` (notificaciones reales, sin duplicar).

## Adaptadores (seccion 7 del TP)

Tres "SDKs externos" simulados (misma idea que `PasarelaPagoLegacy` de la
cátedra: firma de métodos incompatible con lo que el dominio espera) más sus
Adapters:

- **`EmailExternoSdk.despacharCorreo(direccion, asunto, cuerpo)`** →
  `EmailNotificacionAdapter` lo adapta a `CanalNotificacionPort.enviar(destinatarioUserId, mensaje)`.
- **`PushExternoSdk.enviarPush(tokenDispositivo, Map<String,String>)`** →
  `PushNotificacionAdapter` hace lo mismo para push.
- **`NotificacionInternaAdapter`**: sin SDK externo, solo loguea — es el
  canal `INTERNA` por defecto de cualquier `Perfil`.
- **`IdentidadExternaSdk.consultarEstado(userId)`** (devuelve
  `"ACTIVO"`/`"INEXISTENTE"`) → `IdentidadServiceAdapter` lo adapta a
  `IdentidadProviderPort.validarUsuarioActivo(userId)`, que tira
  `IllegalStateException` si el usuario no está activo.

**Simple Factory** (`CanalNotificacionFactoryImpl`) elige el `CanalNotificacionPort`
según el `TipoCanalNotificacion` que pida el caller — no según nada fijo en
código, así que cada `Perfil` puede tener su propio `canalPreferido`
(`EMAIL`/`PUSH`/`INTERNA`, nuevo campo + columna `perfiles.canal_preferido`,
default `INTERNA`).

**Dónde se conecta:**

- `NotificadorObserver` (ver HITO3.md) ahora resuelve el `canalPreferido` de
  cada destinatario contra `PerfilRepository` y pide el canal correspondiente
  a la factory antes de `enviar(...)` — así una misma notificación de dominio
  puede salir por email, push o quedar solo en la bandeja interna según quién
  la reciba.
- `PublicacionUiController`/`ContratacionUiController`/`WalletUiController`
  llaman a `IdentidadProviderPort.validarUsuarioActivo(...)` antes de delegar
  en el caso de uso (crear borrador, cambiar estado, solicitar, avanzar,
  cancelar, acreditar demo) — simula la verificación contra un proveedor de
  identidad externo que pide la secció 7.

**Por qué `NotificadorObserver` se movió de `domain.model.observer` a
`infrastructure.notification`:** para resolver el canal necesita
`CanalNotificacionFactory`, que es un puerto de `application`. Un observer
en `domain` no puede depender de `application` (la dependencia va al revés).
Se lo movió a `infrastructure` y `PublicacionService`/`ContratacionService`
pasaron a recibir la lista de observers ya armada por constructor
(`List<ObservadorDeEstado>`) en vez de construirla ellos mismos — `application`
tampoco puede instanciar una clase de `infrastructure` directamente. Quien
arma la lista concreta es `AppFactory`/`Main`/`MainJpa`, que sí conocen las
tres capas.

Los 36 tests (unitarios + integración JPA) siguen en verde después del
refactor; se corrieron de nuevo end-to-end con los adapters reales
despachando notificaciones (sin duplicados, gracias al fix de HITO3/HITO4
sobre `equals()` de los observers).

## Qué falta

- Pasarela de SkillCoins simulada como Adapter (la sección 7 la menciona
  junto a email/push/identidad, pero no hay pantalla en el Mock UI Kit que
  la dispare — el pago ya se resuelve enteramente en `ContratacionService.pagar`
  moviendo `SkillCoins` entre wallets, sin pasarela externa de por medio).
- Tests automatizados dedicados a `EmailNotificacionAdapter`,
  `PushNotificacionAdapter`, `CanalNotificacionFactoryImpl` e
  `IdentidadServiceAdapter` (hoy se ejercitan indirectamente vía
  `ContratacionServiceTest`/`PublicacionServiceTest`, no hay test que les
  apunte directo).
- Tests automatizados del `presentation.controller` (lo que hay ahora es el
  harness de verificación manual, no quedó como test del proyecto).
- Paginación real en `BusquedaUiController.buscar` (la UI no la pide, pero
  la sección 4.3 del TP la exige).
