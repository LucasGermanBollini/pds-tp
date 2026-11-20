package com.uade.skillswap.domain.model;

import com.uade.skillswap.domain.model.observer.CambioEstadoEvento;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.model.observer.SujetoObservable;
import com.uade.skillswap.domain.model.observer.TipoEventoDominio;
import com.uade.skillswap.domain.model.state.EstadoContratacion;
import com.uade.skillswap.domain.model.state.EstadoContratacionFactory;
import com.uade.skillswap.domain.model.state.EstadoContratacionPendiente;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Solicitud/reserva/contratacion de una publicacion (secciones 4.4 y 4.5).
 *
 * <p>Context del patron State: cada accion se delega al estado actual, que
 * decide si la transicion es valida. Antes de delegar, la contratacion
 * verifica QUIEN pide la accion (seccion 5, permisos): es la experta en esa
 * informacion porque conoce al cliente y, via la publicacion, al proveedor.
 *
 * <p>Subject del patron Observer: cada evento relevante (solicitud,
 * transiciones, reprogramacion) se publica a los observadores, que registran
 * el historial y notifican a las partes sin que esta clase sepa como.
 */
@Entity
@Table(name = "contrataciones")
public class Contratacion implements SujetoObservable {

    /** Estados en los que la contratacion todavia "ocupa" un lugar en la agenda del cliente. */
    private static final Set<String> ESTADOS_ACTIVOS = Set.of("PENDIENTE", "ACEPTADA", "CONFIRMADA", "EN_CURSO");

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_perfil_id", nullable = false)
    private Perfil cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "franja_elegida_id", nullable = false)
    private FranjaDisponibilidad franjaElegida;

    @Column(length = 1000)
    private String mensaje;

    @Column(length = 1000)
    private String objetivo;

    @Column(name = "estado", nullable = false)
    private String estadoNombre = "PENDIENTE";

    @Transient
    private EstadoContratacion estado = new EstadoContratacionPendiente();

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    @Embedded
    private SkillCoins montoPagado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Seccion 4.4: "Una reserva puede reprogramarse antes de comenzar si
     * ambas partes aceptan". Una parte propone (queda guardada la franja
     * propuesta y quien la propuso) y la otra la acepta o la rechaza.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "franja_propuesta_id")
    private FranjaDisponibilidad franjaPropuesta;

    @Column(name = "reprogramacion_propuesta_por", length = 100)
    private String reprogramacionPropuestaPor;

    @Transient
    private List<ObservadorDeEstado> observadores = new ArrayList<>();

    protected Contratacion() {
        // JPA
    }

    public Contratacion(String id, Perfil cliente, Publicacion publicacion, FranjaDisponibilidad franjaElegida,
                         String mensaje, String objetivo) {
        if (cliente == null || publicacion == null || franjaElegida == null) {
            throw new IllegalArgumentException("Cliente, publicacion y franja son obligatorios");
        }
        cliente.verificarActivo();
        // GRASP Information Expert: Contratacion es quien conoce ambos lados para validar esto.
        if (publicacion.getAutor().obtenerId().equals(cliente.obtenerId())) {
            throw new IllegalArgumentException("El autor no puede solicitar su propia publicacion (seccion 5)");
        }
        if (!publicacion.estaPublicada()) {
            throw new IllegalArgumentException("Una publicacion no publicada no admite solicitudes (seccion 5)");
        }
        if (!publicacion.getFranjas().contains(franjaElegida)) {
            throw new IllegalArgumentException("La franja elegida no pertenece a la publicacion");
        }
        if (!franjaElegida.tieneCupo()) {
            throw new IllegalStateException("La franja elegida esta ocupada: no le queda cupo (seccion 5)");
        }
        this.id = id;
        this.cliente = cliente;
        this.publicacion = publicacion;
        this.franjaElegida = franjaElegida;
        this.mensaje = mensaje;
        this.objetivo = objetivo;
        this.fechaCreacion = LocalDateTime.now();
    }

    @PostLoad
    private void reconstruirEstado() {
        this.estado = EstadoContratacionFactory.desdeNombre(this.estadoNombre);
    }

    // ---- permisos (seccion 5) ----

    public String getProveedorId() {
        return publicacion.getAutor().obtenerId();
    }

    public boolean esCliente(String userId) {
        return cliente.obtenerId().equals(userId);
    }

    public boolean esProveedor(String userId) {
        return getProveedorId().equals(userId);
    }

    public boolean esParticipante(String userId) {
        return esCliente(userId) || esProveedor(userId);
    }

    /** La otra parte de la contratacion (a quien se califica, a quien se le pide aceptar una reprogramacion). */
    public String contraparteDe(String userId) {
        verificarParticipante(userId);
        return esCliente(userId) ? getProveedorId() : cliente.obtenerId();
    }

    private void verificarProveedor(String actorUserId, String accion) {
        if (!esProveedor(actorUserId)) {
            throw new AccesoDenegadoException("Solo el proveedor de la publicacion puede " + accion);
        }
    }

    private void verificarCliente(String actorUserId, String accion) {
        if (!esCliente(actorUserId)) {
            throw new AccesoDenegadoException("Solo el cliente que hizo la solicitud puede " + accion);
        }
    }

    public void verificarParticipante(String actorUserId) {
        if (actorUserId == null || !esParticipante(actorUserId)) {
            throw new AccesoDenegadoException("Solo los participantes de la contratacion pueden operar sobre ella");
        }
    }

    // ---- acciones delegadas al State ----

    /** Seccion 4.6: "solicitud recibida". La llama el caso de uso una vez guardada la contratacion. */
    public void notificarSolicitudRecibida() {
        notificar(new CambioEstadoEvento(TipoEventoDominio.SOLICITUD_RECIBIDA, "CONTRATACION", id, null, estadoNombre,
                cliente.obtenerId(), null, cliente.obtenerNombreVisible() + " solicito '" + publicacion.getTitulo() + "'",
                List.of(getProveedorId()), LocalDateTime.now()));
    }

    public void aceptar(String actorUserId) {
        verificarProveedor(actorUserId, "aceptar la solicitud");
        estado.aceptar(this, actorUserId);
    }

    public void rechazar(String actorUserId) {
        verificarProveedor(actorUserId, "rechazar la solicitud");
        estado.rechazar(this, actorUserId);
    }

    /**
     * Solo transiciona ACEPTADA -> CONFIRMADA: el movimiento de SkillCoins lo
     * hace el caso de uso (toca dos wallets). Se llama ANTES de mover dinero
     * para que un segundo pago (doble cobro) falle sin tocar ningun saldo.
     */
    public void pagar(String actorUserId) {
        verificarCliente(actorUserId, "pagar la contratacion");
        if (montoPagado != null) {
            throw new IllegalStateException("La contratacion ya fue pagada: no se permite doble cobro (seccion 4.4)");
        }
        estado.pagar(this, actorUserId);
    }

    /** El movimiento de SkillCoins ya ocurrio en el caso de uso; esto solo deja constancia del monto. */
    public void registrarMontoPagado(SkillCoins monto) {
        this.montoPagado = monto;
    }

    public SkillCoins getMontoPagado() {
        return montoPagado;
    }

    public void iniciar(String actorUserId) {
        verificarProveedor(actorUserId, "iniciar la sesion");
        estado.iniciar(this, actorUserId);
    }

    public void completar(String actorUserId) {
        verificarProveedor(actorUserId, "marcar la sesion como completada");
        estado.completar(this, actorUserId);
    }

    /**
     * A diferencia de Publicacion.cancelar() (seccion 4.2: "motivo obligatorio"),
     * aca el motivo es opcional: el TP no lo exige para contrataciones, y el
     * Mock UI Kit de la catedra (ContratacionController.cancelar) no lo pide.
     */
    public void cancelar(String actorUserId, String motivo) {
        verificarParticipante(actorUserId);
        String motivoFinal = (motivo == null || motivo.isBlank()) ? "Sin motivo especificado" : motivo;
        estado.cancelar(this, actorUserId, motivoFinal);
        this.motivoCancelacion = motivoFinal;
        limpiarPropuestaDeReprogramacion();
    }

    /** Seccion 4.5: COMPLETADA habilita "Calificar". Creator (GRASP): la contratacion crea su calificacion. */
    public Calificacion calificar(String calificacionId, String autorUserId, int puntaje, String comentario) {
        verificarParticipante(autorUserId);
        estado.calificar(this, autorUserId);
        return new Calificacion(calificacionId, id, autorUserId, contraparteDe(autorUserId), puntaje, comentario,
                LocalDateTime.now());
    }

    // ---- reprogramacion consensuada (seccion 4.4) ----

    public void proponerReprogramacion(String actorUserId, FranjaDisponibilidad nuevaFranja) {
        verificarParticipante(actorUserId);
        estado.proponerReprogramacion(this, actorUserId, nuevaFranja);
    }

    public void aceptarReprogramacion(String actorUserId) {
        verificarParticipante(actorUserId);
        if (franjaPropuesta == null) {
            throw new IllegalStateException("No hay ninguna reprogramacion propuesta");
        }
        if (actorUserId.equals(reprogramacionPropuestaPor)) {
            throw new AccesoDenegadoException("La reprogramacion la tiene que aceptar la otra parte");
        }
        estado.aceptarReprogramacion(this, actorUserId);
    }

    /** Quien recibio la propuesta la rechaza, o quien la hizo la retira. */
    public void rechazarReprogramacion(String actorUserId) {
        verificarParticipante(actorUserId);
        if (franjaPropuesta == null) {
            throw new IllegalStateException("No hay ninguna reprogramacion propuesta");
        }
        FranjaDisponibilidad rechazada = franjaPropuesta;
        limpiarPropuestaDeReprogramacion();
        notificar(evento(TipoEventoDominio.REPROGRAMACION_RECHAZADA, estadoNombre, estadoNombre, actorUserId, null,
                "Se rechazo la reprogramacion de '" + publicacion.getTitulo() + "' al " + rechazada.getFechaHoraInicio()));
    }

    /** Lo invocan ACEPTADA/CONFIRMADA (antes de comenzar). Valida la franja pero todavia no reserva cupo. */
    public void registrarPropuestaDeReprogramacion(String actorUserId, FranjaDisponibilidad nuevaFranja) {
        if (nuevaFranja == null) {
            throw new IllegalArgumentException("La nueva franja es obligatoria");
        }
        if (nuevaFranja.equals(franjaElegida)) {
            throw new IllegalArgumentException("La nueva franja es la misma que la actual");
        }
        if (!publicacion.getFranjas().contains(nuevaFranja)) {
            throw new IllegalArgumentException("La nueva franja no pertenece a la publicacion");
        }
        if (!nuevaFranja.tieneCupo()) {
            throw new IllegalStateException("La nueva franja esta ocupada: no le queda cupo");
        }
        if (franjaPropuesta != null) {
            throw new IllegalStateException("Ya hay una reprogramacion pendiente de respuesta");
        }
        this.franjaPropuesta = nuevaFranja;
        this.reprogramacionPropuestaPor = actorUserId;
        notificar(evento(TipoEventoDominio.REPROGRAMACION_PROPUESTA, estadoNombre, estadoNombre, actorUserId, null,
                "Se propuso reprogramar '" + publicacion.getTitulo() + "' al " + nuevaFranja.getFechaHoraInicio()));
    }

    /** Lo invocan ACEPTADA/CONFIRMADA cuando la otra parte acepta: mueve el cupo de una franja a la otra. */
    public void aplicarReprogramacion(String actorUserId) {
        FranjaDisponibilidad nueva = franjaPropuesta;
        if (!nueva.reservarCupo()) {
            throw new IllegalStateException("No hay cupo disponible en la nueva franja");
        }
        franjaElegida.liberarCupo();
        this.franjaElegida = nueva;
        limpiarPropuestaDeReprogramacion();
        notificar(evento(TipoEventoDominio.REPROGRAMACION_ACEPTADA, estadoNombre, estadoNombre, actorUserId, null,
                "'" + publicacion.getTitulo() + "' quedo reprogramada al " + nueva.getFechaHoraInicio()));
    }

    private void limpiarPropuestaDeReprogramacion() {
        this.franjaPropuesta = null;
        this.reprogramacionPropuestaPor = null;
    }

    /** Invocado solo por los estados concretos (viven en otro paquete, por eso es publico). */
    public void cambiarEstado(EstadoContratacion nuevo, String actorUserId, String motivo) {
        String anterior = this.estadoNombre;
        this.estado = nuevo;
        this.estadoNombre = nuevo.getNombre();
        notificar(evento(TipoEventoDominio.CAMBIO_ESTADO, anterior, estadoNombre, actorUserId, motivo,
                "La contratacion de '" + publicacion.getTitulo() + "' paso de " + anterior + " a " + estadoNombre));
    }

    private CambioEstadoEvento evento(TipoEventoDominio tipo, String anterior, String nuevo, String actorUserId,
                                      String motivo, String descripcion) {
        return new CambioEstadoEvento(tipo, "CONTRATACION", id, anterior, nuevo, actorUserId, motivo, descripcion,
                List.of(cliente.obtenerId(), getProveedorId()), LocalDateTime.now());
    }

    // ---- consultas ----

    /** PENDIENTE..EN_CURSO: todavia ocupa la agenda del cliente. */
    public boolean estaActiva() {
        return ESTADOS_ACTIVOS.contains(estadoNombre);
    }

    // ---- Observer (Subject) ----

    @Override
    public void agregarObservador(ObservadorDeEstado observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    @Override
    public void quitarObservador(ObservadorDeEstado observador) {
        observadores.remove(observador);
    }

    @Override
    public void notificar(CambioEstadoEvento evento) {
        for (ObservadorDeEstado observador : observadores) {
            observador.actualizar(evento);
        }
    }

    // ---- getters ----

    public String getId() {
        return id;
    }

    public Perfil getCliente() {
        return cliente;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public FranjaDisponibilidad getFranjaElegida() {
        return franjaElegida;
    }

    public FranjaDisponibilidad getFranjaPropuesta() {
        return franjaPropuesta;
    }

    public String getReprogramacionPropuestaPor() {
        return reprogramacionPropuestaPor;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public String getEstadoNombre() {
        return estadoNombre;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
