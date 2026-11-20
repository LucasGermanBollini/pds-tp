# Hito 4 — plan de adaptación al Mock UI Kit

El profe subió `0. TP_PDS_SkillSwap/skillswap` (interfaz Swing + contratos de
Controllers). Ya está **fusionado** dentro de este mismo proyecto Maven
(`src/main/java/ar/edu/uade/skillswap/...`, al lado de nuestro
`com.uade.skillswap...`), compila junto y sus 2 tests pasan. Esto documenta
qué va a hacer falta para conectar los Controllers reales — el trabajo en sí
es Hito 4, todavía no lo implementé.

## Ya corregido ahora (compatibilidad barata, sin esperar a Hito 4)

- **Versiones de dependencias alineadas** al `pom.xml` del kit: Hibernate
  7.2.1.Final (antes 6.5.2.Final), mysql-connector-j 9.7.0, flyway-mysql
  12.8.1, junit-jupiter 6.1.0. Recompilé y corrí los 36 tests — sin
  roturas por el salto de versión.
- **`Modalidad` le faltaba `HIBRIDA`**: `Models.Modalidad` del kit tiene
  `REMOTA, PRESENCIAL, HIBRIDA`; la nuestra solo tenía las primeras dos.
  Agregada en el enum y en `V1__init.sql`.
- **`Contratacion.cancelar()` exigía motivo obligatorio**: el TP solo pide
  motivo obligatorio para cancelar una *Publicacion* (sección 4.2); para
  Contratacion me lo había autoimpuesto. `ContratacionController.cancelar`
  del kit no pide motivo, así que lo volví opcional (default "Sin motivo
  especificado" si no lo dan).

## Lo que va a hacer falta en Hito 4 (Controllers reales)

El contrato real (`UiControllers`) es más chico y "plano" que nuestro
dominio — eso es intencional (es una interfaz de UI, no el dominio), pero
implica un **adaptador** en `presentation.controller` por cada Controller,
que traduce entre los records de `Models` y nuestras entidades/casos de uso.
Puntos concretos:

| Método del kit | Diferencia con nuestro dominio | Cómo se resuelve |
|---|---|---|
| `PublicacionController.cambiarEstado(userId, id, EstadoPublicacion)` | Un solo método genérico; nosotros tenemos `publicar/pausar/cerrar/cancelar` separados | El adapter mapea el `EstadoPublicacion` destino a la acción correspondiente |
| `ContratacionController.avanzar(userId, id)` | "Avanzar al siguiente estado" genérico; nosotros tenemos `aceptar/pagar/iniciar/completar` | El adapter mira `getEstadoNombre()` actual y llama la acción que corresponde (ACEPTADA→pagar necesita además construir el `Cotizable` del precio) |
| `ContratacionController.solicitar(userId, publicacionId, objetivo)` | Sin franja ni mensaje | El adapter elige automáticamente una franja con cupo disponible de la publicación; `objetivo` se mapea directo, `mensaje` queda vacío o un default |
| `PublicacionController.crearBorrador(..., categoria: String, ...)` | `categoria` es un String plano, no `ElementoCatalogo` | El adapter hace *find-or-create* de una `CategoriaHabilidad` por nombre contra `ElementoCatalogoRepository` |
| `crearBorrador` no manda franjas | `validarRequisitosDePublicacion()` exige al menos una | El adapter crea una franja default (ej. "en 7 días", cupo configurable) al crear el borrador, editable después |
| `WalletController.acreditarDemo(userId, importe)` | No existe un caso de uso así | Agregar `ContratacionService.acreditarDemo(...)` (o un `WalletService` nuevo): acredita + registra `MovimientoCreditos` |
| `WalletController.consultarSaldo` devuelve `int` | Nuestro `SkillCoins` envuelve `long` | Cast directo en el adapter |
| `PerfilController.verPerfil` pide `rol`, `resenas`, `proyectosCompletados` | No los tenemos todos | `resenas` = `cantidadCalificaciones` (ya existe); `proyectosCompletados` = contar Contrataciones COMPLETADA del usuario (nueva query); `rol` = agregar un campo corto a `Perfil` (ej. `rolDescripcion`) |

## Qué NO hace falta tocar

- `EstadoPublicacion` y `EstadoContratacion` del kit coinciden **exactos**
  con los nuestros (mismos nombres, mismo orden) — el State pattern no
  necesita ningún cambio.
- El kit no tiene assets externos (sin imágenes/fuentes): el merge fue solo
  código, sin recursos que copiar.
- `SkillSwapApplication` ya es el `mainClass` configurado en `pom.xml`
  (`exec:java` lo levanta directo); nuestros `Main`/`MainJpa` del Hito 2/3
  siguen andando pasando `-Dexec.mainClass=...` o `java -cp ... <clase>`.

## Screenshot de referencia

La UI ya levanta tal cual vino, fusionada en este proyecto (capturado con
`SkillSwapApplication --screenshot`, pantalla "Inicio" con datos mock).
