package com.uade.skillswap.domain.model;

import com.uade.skillswap.domain.model.observer.CambioEstadoEvento;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.model.observer.SujetoObservable;
import com.uade.skillswap.domain.model.state.EstadoPublicacion;
import com.uade.skillswap.domain.model.state.EstadoPublicacionBorrador;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Publicacion implements SujetoObservable {

    private final String id;
    private final Publicador autor;
    private String titulo;
    private String descripcion;
    private ElementoCatalogo categoria;
    private Modalidad modalidad;
    private int duracionEstimadaMinutos;
    private SkillCoins precioBase;
    private final List<Habilidad> etiquetas = new ArrayList<>();
    private final List<FranjaDisponibilidad> franjas = new ArrayList<>();
    private EstadoPublicacion estado = new EstadoPublicacionBorrador();
    private int contadorSolicitudes = 0;

    private final List<ObservadorDeEstado> observadores = new ArrayList<>();

    public Publicacion(String id, Publicador autor, String titulo) {
        if (autor == null) {
            throw new IllegalArgumentException("La publicacion necesita un autor");
        }
        this.id = id;
        this.autor = autor;
        this.titulo = titulo;
    }

    // ---- acciones delegadas al State ----

    public void publicar(String actorUserId) {
        estado.publicar(this, actorUserId);
    }

    public void pausar(String actorUserId) {
        estado.pausar(this, actorUserId);
    }

    public void cerrar(String actorUserId) {
        estado.cerrar(this, actorUserId);
    }

    public void cancelar(String actorUserId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Cancelar una publicacion requiere motivo (seccion 4.2)");
        }
        estado.cancelar(this, actorUserId, motivo);
    }

    /** Invocado por los estados concretos; publico porque viven en otro paquete. */
    public void cambiarEstado(EstadoPublicacion nuevo, String actorUserId, String motivo) {
        String anterior = this.estado.getNombre();
        this.estado = nuevo;
        notificar(new CambioEstadoEvento("PUBLICACION", id, anterior, nuevo.getNombre(), actorUserId, motivo, LocalDateTime.now()));
    }

    /** Seccion 4.2: "Publicar requiere titulo, categoria, precio no negativo y al menos una franja". */
    public void validarRequisitosDePublicacion() {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("La publicacion necesita titulo");
        }
        if (categoria == null) {
            throw new IllegalArgumentException("La publicacion necesita categoria");
        }
        if (precioBase == null || precioBase.esNegativo()) {
            throw new IllegalArgumentException("La publicacion necesita un precio base no negativo");
        }
        if (franjas.isEmpty()) {
            throw new IllegalArgumentException("La publicacion necesita al menos una franja de disponibilidad");
        }
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

    // ---- edicion mientras esta en BORRADOR/PAUSADA ----

    public void completarDatos(String descripcion, ElementoCatalogo categoria, Modalidad modalidad,
                                int duracionEstimadaMinutos, SkillCoins precioBase) {
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.modalidad = modalidad;
        this.duracionEstimadaMinutos = duracionEstimadaMinutos;
        this.precioBase = precioBase;
    }

    public void agregarEtiqueta(Habilidad habilidad) {
        if (habilidad != null && !etiquetas.contains(habilidad)) {
            etiquetas.add(habilidad);
        }
    }

    public void agregarFranja(FranjaDisponibilidad franja) {
        if (franja != null) {
            franjas.add(franja);
        }
    }

    public void registrarSolicitud() {
        contadorSolicitudes++;
    }

    // ---- getters ----

    public String getId() {
        return id;
    }

    public Publicador getAutor() {
        return autor;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public ElementoCatalogo getCategoria() {
        return categoria;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public SkillCoins getPrecioBase() {
        return precioBase;
    }

    public List<Habilidad> getEtiquetas() {
        return List.copyOf(etiquetas);
    }

    public List<FranjaDisponibilidad> getFranjas() {
        return List.copyOf(franjas);
    }

    public String getEstadoNombre() {
        return estado.getNombre();
    }

    public int getContadorSolicitudes() {
        return contadorSolicitudes;
    }
}
