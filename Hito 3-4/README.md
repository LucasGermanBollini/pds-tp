# SkillSwap — Trabajo Práctico Obligatorio (PDS)

Backend en **Java 17 + Hibernate/JPA + MySQL** (sin Spring Boot), con migraciones **Flyway**, conectado a la
interfaz Swing de la cátedra (Mock UI Kit). Versión final: cubre las secciones 1 a 16 de la consigna.

| Qué | Dónde |
|---|---|
| Código fuente | `src/main/java/com/uade/skillswap` |
| Interfaz de la cátedra (Mock UI Kit) | `src/main/java/ar/edu/uade/skillswap` |
| Migraciones (esquema + datos demo) | `src/main/resources/db/migration` (V1, V2, V3) |
| Tests (107) | `src/test/java` |
| UML en StarUML (9 diagramas) | `docs/SkillSwap - Modelo Final.mdj` (imágenes en `docs/diagramas`) |
| Informe de decisiones (PDF) | `docs/Informe Final - SkillSwap.pdf` |
| Capturas de la interfaz conectada | `docs/capturas` |
| Notas de los hitos anteriores | `docs/historial` |

## Cómo correrlo

Requisitos: JDK 17 o superior y Maven 3.9. Los comandos se corren desde esta carpeta (`Hito 3-4`).

```bash
mvn test
```
Compila, corre los 107 tests (unitarios, de casos de uso, de controllers y de persistencia sobre H2) y genera
el reporte de cobertura en `target/site/jacoco/index.html`. Falla si dominio o aplicación bajan del 80 %.

```bash
mvn -q exec:java -Dexec.mainClass=com.uade.skillswap.SkillSwapRealApplication -Dexec.args=--h2
```
Abre la **interfaz con los controllers reales** sobre una base H2 en memoria (mismas migraciones Flyway que
MySQL, no hay que instalar nada). Al entrar se elige el usuario (login simulado):

- `user-mateo` — Mateo Silva, profesional (el usuario del Mock UI Kit).
- `prof-sofia` — Sofía Méndez, mentora.
- `org-techcorp` — TechCorp, organización (publica desafíos).

Con `-Dexec.args=--memoria` usa repositorios en memoria. Sin argumentos usa **MySQL**; antes hay que crear la base
y el usuario una sola vez:

```bash
mysql -u root -p < scripts/bootstrap-mysql.sql
```

```bash
mvn -q exec:java -Dexec.mainClass=com.uade.skillswap.SkillSwapRealApplication
```

Recorrido de la demo por consola (sección 13: publicar → solicitar → aceptar → pagar → completar → calificar,
más casos negativos), en memoria o con JPA:

```bash
mvn -q exec:java -Dexec.mainClass=com.uade.skillswap.Main
```
```bash
mvn -q exec:java -Dexec.mainClass=com.uade.skillswap.MainJpa -Dexec.args=--h2
```

### Demo en la interfaz, paso a paso

1. Entrar como **Mateo** → *Explorar* → "Preparación para entrevistas técnicas" → *Solicitar mentoría*.
2. Menú *Sesión → Cambiar de usuario* → **Sofía** → *Contrataciones* → *Avanzar estado* (acepta y bloquea el cupo).
3. Volver a **Mateo** → *Avanzar estado* (paga con SkillCoins: el saldo baja de 2480 a 2300).
4. **Sofía** → *Avanzar estado* dos veces (inicia y completa).
5. Menú *Acciones → Calificar una contratación completada* (como Mateo y como Sofía).
6. Probar errores: como Mateo intentar aceptar ("No tenés permiso"), pagar dos veces, calificar dos veces.
7. *Acciones → Proponer reprogramación* (una parte) y *Responder reprogramación* (la otra).

## Arquitectura (sección 6)

```
presentation ──► application ──► domain ◄── infrastructure
      ▲                ▲                          │
      └──────── config (Composition Root) ────────┘
```

- **domain** (`domain.model`, `domain.model.state`, `domain.model.observer`, `domain.repository`): entidades,
  value objects (`SkillCoins`, `DisponibilidadSemanal`), reglas, estados, eventos y los puertos de repositorio.
  No conoce la interfaz ni las otras capas. Lleva las anotaciones JPA, igual que el ejemplo de la cátedra.
- **application** (`usecase`, `port`, `dto`, `excepcion`): casos de uso (`PublicacionService`,
  `ContratacionService`, `PerfilService`, `WalletService`, `NotificacionService`), la Facade
  `PerfilPublicoFacade`, los puertos `UnitOfWork`, `IdentidadProviderPort`, `CanalNotificacionPort` y
  `CanalNotificacionFactory`, y los DTOs de salida.
- **infrastructure**: repositorios JPA y en memoria, `JpaUnitOfWork` (transacciones), `JPAUtil` (única
  `EntityManagerFactory`), adaptadores de email, push e identidad, y el observer que notifica.
- **presentation**: los controllers de la sección 9 (`presentation.controller`) y los adaptadores del Mock UI Kit
  (`presentation.ui`).
- **config**: `AppFactory` arma todo el grafo a mano; `Repositorios` permite cambiar de memoria a JPA en una línea.

## Patrones (sección 7)

| Patrón | Dónde | Qué variación resuelve |
|---|---|---|
| State | `EstadoPublicacion*`, `EstadoContratacion*` | Comportamiento y transiciones válidas de cada estado |
| Strategy | `RecomendacionStrategy`, `PoliticaCancelacionStrategy` | Recomendación (afinidad/popularidad) y reintegro (estándar/flexible) |
| Observer | `SujetoObservable`, `RegistradorHistorialObserver`, `NotificadorObserver` | Historial y notificaciones ante cada evento, sin acoplar el dominio |
| Adapter | `Email/Push/NotificacionInternaAdapter`, `IdentidadServiceAdapter`, `*UiController` | SDKs externos incompatibles y contrato del Mock UI Kit |
| Simple Factory | `CanalNotificacionFactoryImpl`, `RecomendacionStrategyFactory`, `Estado*Factory` | Elegir canal, estrategia o reconstruir el estado persistido |
| Decorator | `PromocionDecorator`, `DescuentoPorcentual`, `DescuentoMontoFijo` | Promociones acumulables sin precio negativo |
| Composite | `ElementoCatalogo`, `Habilidad`, `CategoriaHabilidad` | Catálogo jerárquico de categorías y habilidades |
| Facade | `PerfilPublicoFacade` | Vista resumida del perfil a partir de cuatro repositorios |
| Singleton | `JPAUtil` | Una sola `EntityManagerFactory` (costosa de crear) |

## Persistencia (sección 8)

- Esquema versionado con Flyway (`V1__init.sql`, `V2__seed_demo_data.sql`, `V3__...sql`); Hibernate solo
  **valida** (`hbm2ddl.auto=validate`).
- Transacciones con `JpaUnitOfWork` en aceptar, reservar, pagar, cancelar y reprogramar.
- **Optimistic locking** con `@Version` en `FranjaDisponibilidad` (evita sobreventa del último cupo); el
  conflicto llega a la interfaz como `CONFLICTO_CONCURRENTE`.
- Búsqueda paginada en la base: ids con filtros + `ORDER BY` + `LIMIT/OFFSET`, `COUNT` y carga con
  `JOIN FETCH`; el resto de las colecciones se carga en lotes (`hibernate.default_batch_fetch_size`). Un test
  verifica que la cantidad de consultas no crece con la cantidad de resultados (sin N+1).
- Restricciones de unicidad (una calificación por autor y contratación, una wallet por usuario, etc.) e
  índices para las consultas principales (`V3`).

## Errores uniformes (sección 9)

Todos los controllers devuelven `Resultado<T>`: un valor o un código de error estable más un mensaje.

| Código | Cuándo |
|---|---|
| `DATOS_INVALIDOS` | Falta un dato o tiene un valor inválido (precio negativo, puntaje fuera de 1–5…) |
| `NO_ENCONTRADO` | El id no existe |
| `ACCESO_DENEGADO` | El usuario no puede hacer esa acción (no es el autor, el proveedor, el cliente…) |
| `OPERACION_NO_PERMITIDA` | Regla de negocio o transición inválida (saldo insuficiente, sin cupo, doble pago…) |
| `CONFLICTO_CONCURRENTE` | Otra transacción modificó lo mismo antes (optimistic locking) |
| `ERROR_INTERNO` | Error inesperado (se registra en el log; no se muestran detalles internos) |
