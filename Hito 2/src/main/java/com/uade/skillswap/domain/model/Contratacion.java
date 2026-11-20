package com.uade.skillswap.domain.model;

import com.uade.skillswap.domain.model.observer.CambioEstadoEvento;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.model.observer.SujetoObservable;
import com.uade.skillswap.domain.model.state.EstadoContratacion;
import com.uade.skillswap.domain.model.state.EstadoContratacionPendiente;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Contratacion implements SujetoObservable {

    private final String id;
    private final Perfil cliente;
    private final Publicacion publicacion;
    private FranjaDisponibilidad franjaElegida;
    private final String mensaje;
    private final String objetivo;
    private EstadoContratacion estado = new EstadoContratacionPendiente();
    private String motivoCancelacion;
    private SkillCoins montoPagado;
    private final LocalDateTime fechaCreacion;

    private final List<ObservadorDeEstado> observadores = new ArrayList<>();

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

    public void cancelar(String actorUserId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Cancelar una contratacion requiere motivo");
        }
        this.motivoCancelacion = motivo;
        estado.cancelar(this, actorUserId, motivo);
    }

    public void proponerReprogramacion(String actorUserId, FranjaDisponibilidad nuevaFranja) {
        estado.proponerReprogramacion(this, actorUserId, nuevaFranja);
    }

    /** Invocado solo por los estados concretos (viven en otro paquete, por eso es publico). */
    public void cambiarEstado(EstadoContratacion nuevo, String actorUserId, String motivo) {
        String anterior = this.estado.getNombre();
        this.estado = nuevo;
        notificar(new CambioEstadoEvento("CONTRATACION", id, anterior, nuevo.getNombre(), actorUserId, motivo, LocalDateTime.now()));
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
        return estado.getNombre();
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
