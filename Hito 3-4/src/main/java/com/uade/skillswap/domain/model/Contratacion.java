package com.uade.skillswap.domain.model;

import com.uade.skillswap.domain.model.observer.CambioEstadoEvento;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.model.observer.SujetoObservable;
import com.uade.skillswap.domain.model.state.EstadoContratacion;
import com.uade.skillswap.domain.model.state.EstadoContratacionFactory;
import com.uade.skillswap.domain.model.state.EstadoContratacionPendiente;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contrataciones")
public class Contratacion implements SujetoObservable {

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
        // GRASP Information Expert: Contratacion es quien conoce ambos lados para validar esto.
        if (publicacion.getAutor().obtenerId().equals(cliente.obtenerId())) {
            throw new IllegalArgumentException("El autor no puede solicitar su propia publicacion (seccion 5)");
        }
        if (!"PUBLICADA".equals(publicacion.getEstadoNombre())) {
            throw new IllegalArgumentException("Una publicacion no publicada no admite solicitudes (seccion 5)");
        }
        if (!publicacion.getFranjas().contains(franjaElegida)) {
            throw new IllegalArgumentException("La franja elegida no pertenece a la publicacion");
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

    // ---- acciones delegadas al State ----

    public void aceptar(String actorUserId) {
        estado.aceptar(this, actorUserId);
    }

    public void rechazar(String actorUserId) {
        estado.rechazar(this, actorUserId);
    }

    public void pagar(String actorUserId) {
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
        estado.iniciar(this, actorUserId);
    }

    public void completar(String actorUserId) {
        estado.completar(this, actorUserId);
    }

    /**
     * A diferencia de Publicacion.cancelar() (seccion 4.2: "motivo obligatorio"),
     * aca el motivo es opcional: el TP no lo exige para contrataciones, y el
     * Mock UI Kit de la catedra (ContratacionController.cancelar) no lo pide.
     */
    public void cancelar(String actorUserId, String motivo) {
        String motivoFinal = (motivo == null || motivo.isBlank()) ? "Sin motivo especificado" : motivo;
        this.motivoCancelacion = motivoFinal;
        estado.cancelar(this, actorUserId, motivoFinal);
    }

    public void proponerReprogramacion(String actorUserId, FranjaDisponibilidad nuevaFranja) {
        estado.proponerReprogramacion(this, actorUserId, nuevaFranja);
    }

    /** Invocado solo por los estados concretos (viven en otro paquete, por eso es publico). */
    public void cambiarEstado(EstadoContratacion nuevo, String actorUserId, String motivo) {
        String anterior = this.estadoNombre;
        this.estado = nuevo;
        this.estadoNombre = nuevo.getNombre();
        notificar(new CambioEstadoEvento("CONTRATACION", id, anterior, this.estadoNombre, actorUserId, motivo, LocalDateTime.now()));
    }

    /** Usado por los estados ACEPTADA/CONFIRMADA para reprogramar sin cambiar de estado. */
    public void reprogramar(FranjaDisponibilidad nuevaFranja) {
        if (nuevaFranja == null) {
            throw new IllegalArgumentException("La nueva franja es obligatoria");
        }
        if (!nuevaFranja.reservarCupo()) {
            throw new IllegalStateException("No hay cupo disponible en la nueva franja");
        }
        franjaElegida.liberarCupo();
        this.franjaElegida = nuevaFranja;
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
