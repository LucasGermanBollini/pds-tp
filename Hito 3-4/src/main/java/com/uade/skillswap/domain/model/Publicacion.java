package com.uade.skillswap.domain.model;

import com.uade.skillswap.domain.model.observer.CambioEstadoEvento;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.model.observer.SujetoObservable;
import com.uade.skillswap.domain.model.observer.TipoEventoDominio;
import com.uade.skillswap.domain.model.state.EstadoPublicacion;
import com.uade.skillswap.domain.model.state.EstadoPublicacionBorrador;
import com.uade.skillswap.domain.model.state.EstadoPublicacionFactory;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publicaciones")
public class Publicacion implements SujetoObservable {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(targetEntity = Perfil.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_perfil_id", nullable = false)
    private Publicador autor;

    /** OFERTA (profesional) o DESAFIO (organizacion), seccion 4.2. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_publicacion", nullable = false)
    private TipoPublicacion tipo = TipoPublicacion.OFERTA;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 2000)
    private String descripcion;

    /**
     * @ManyToOne porque una categoria puede agrupar muchas publicaciones.
     * EAGER por el mismo motivo que etiquetas/franjas: se lee seguido justo
     * despues de un buscarPorId() ya desconectado de la sesion que cargo.
     */
    @ManyToOne(targetEntity = ElementoCatalogoBase.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private ElementoCatalogo categoria;

    @Enumerated(EnumType.STRING)
    private Modalidad modalidad;

    @Column(name = "duracion_estimada_minutos", nullable = false)
    private int duracionEstimadaMinutos;

    @Embedded
    private SkillCoins precioBase;

    /*
     * EAGER a proposito: el dominio llama agregarEtiqueta()/agregarFranja()
     * sobre entidades que ya volvieron de buscarPorId() (fuera de la sesion
     * que las cargo, porque cada AbstractJpaRepository.guardar()/find()
     * abre y cierra su propio EntityManager por operacion, como
     * SimpleJpaRepository en la catedra). Con LAZY, tocar la coleccion
     * despues de ese cierre tira LazyInitializationException. Para el
     * listado masivo (buscarPublicadasConFranjas) se usa JOIN FETCH
     * explicito, que es donde el N+1 realmente importa (muchas filas).
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "publicacion_habilidades",
            joinColumns = @JoinColumn(name = "publicacion_id"),
            inverseJoinColumns = @JoinColumn(name = "habilidad_id"))
    private List<Habilidad> etiquetas = new ArrayList<>();

    /** mappedBy "publicacion": FranjaDisponibilidad es quien tiene la FK. Ver nota de EAGER en `etiquetas`. */
    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FranjaDisponibilidad> franjas = new ArrayList<>();

    /*
     * State persistido como texto (igual que EstadoPedido/Pedido.estado de
     * la catedra); el objeto State es comportamiento de dominio en memoria.
     */
    @Column(name = "estado", nullable = false)
    private String estadoNombre = "BORRADOR";

    @Transient
    private EstadoPublicacion estado = new EstadoPublicacionBorrador();

    @Column(name = "contador_solicitudes", nullable = false)
    private int contadorSolicitudes = 0;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    /** Se fija la primera vez que pasa a PUBLICADA; permite ordenar por "fecha mas reciente" (seccion 4.3). */
    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    /** Los observers son comportamiento de runtime (Hito 2/3); no se persisten. */
    @Transient
    private List<ObservadorDeEstado> observadores = new ArrayList<>();

    protected Publicacion() {
        // JPA
    }

    public Publicacion(String id, Publicador autor, String titulo) {
        this(id, autor, titulo, TipoPublicacion.OFERTA);
    }

    public Publicacion(String id, Publicador autor, String titulo, TipoPublicacion tipo) {
        if (autor == null) {
            throw new IllegalArgumentException("La publicacion necesita un autor");
        }
        TipoPublicacion tipoFinal = tipo == null ? TipoPublicacion.OFERTA : tipo;
        if (!autor.estaActivo()) {
            throw new IllegalStateException("Un perfil dado de baja no puede publicar");
        }
        if (!autor.puedePublicar(tipoFinal)) {
            throw new IllegalArgumentException("Este perfil no puede crear una publicacion de tipo " + tipoFinal
                    + " (seccion 4.2: los profesionales publican ofertas y las organizaciones, desafios)");
        }
        this.id = id;
        this.autor = autor;
        this.titulo = titulo;
        this.tipo = tipoFinal;
        this.fechaCreacion = LocalDateTime.now();
    }

    @PostLoad
    private void reconstruirEstado() {
        this.estado = EstadoPublicacionFactory.desdeNombre(this.estadoNombre);
    }

    // ---- acciones delegadas al State ----

    public void publicar(String actorUserId) {
        verificarAutor(actorUserId);
        if (!autor.estaActivo()) {
            throw new IllegalStateException("Un perfil dado de baja no puede publicar");
        }
        estado.publicar(this, actorUserId);
    }

    public void pausar(String actorUserId) {
        verificarAutor(actorUserId);
        estado.pausar(this, actorUserId);
    }

    public void cerrar(String actorUserId) {
        verificarAutor(actorUserId);
        estado.cerrar(this, actorUserId);
    }

    public void cancelar(String actorUserId, String motivo) {
        verificarAutor(actorUserId);
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Cancelar una publicacion requiere motivo (seccion 4.2)");
        }
        estado.cancelar(this, actorUserId, motivo);
    }

    /**
     * Seccion 5: permisos a partir del userId. Solo el autor gestiona su
     * publicacion (GRASP Information Expert: la publicacion conoce a su autor).
     */
    public void verificarAutor(String actorUserId) {
        if (actorUserId == null || !autor.obtenerId().equals(actorUserId)) {
            throw new AccesoDenegadoException("Solo el autor puede modificar la publicacion '" + titulo + "'");
        }
    }

    public boolean esAutor(String userId) {
        return autor.obtenerId().equals(userId);
    }

    public boolean estaPublicada() {
        return "PUBLICADA".equals(estadoNombre);
    }

    /** Invocado por los estados concretos; publico porque viven en otro paquete. */
    public void cambiarEstado(EstadoPublicacion nuevo, String actorUserId, String motivo) {
        String anterior = this.estadoNombre;
        this.estado = nuevo;
        this.estadoNombre = nuevo.getNombre();
        if (estaPublicada() && fechaPublicacion == null) {
            fechaPublicacion = LocalDateTime.now();
        }
        String descripcion = "Tu publicacion '" + titulo + "' paso de " + anterior + " a " + estadoNombre;
        notificar(new CambioEstadoEvento(TipoEventoDominio.CAMBIO_ESTADO, "PUBLICACION", id, anterior, estadoNombre,
                actorUserId, motivo, descripcion, List.of(autor.obtenerId()), LocalDateTime.now()));
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

    /** Seccion 4.2: "BORRADOR: editable". Tambien se permite editar una PAUSADA (no recibe solicitudes). */
    private void verificarEditable() {
        if (!"BORRADOR".equals(estadoNombre) && !"PAUSADA".equals(estadoNombre)) {
            throw new IllegalStateException("Solo se puede editar una publicacion en BORRADOR o PAUSADA (estado actual: "
                    + estadoNombre + ")");
        }
    }

    public void completarDatos(String descripcion, ElementoCatalogo categoria, Modalidad modalidad,
                                int duracionEstimadaMinutos, SkillCoins precioBase) {
        verificarEditable();
        if (duracionEstimadaMinutos < 0) {
            throw new IllegalArgumentException("La duracion estimada no puede ser negativa");
        }
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.modalidad = modalidad;
        this.duracionEstimadaMinutos = duracionEstimadaMinutos;
        this.precioBase = precioBase;
    }

    public void agregarEtiqueta(Habilidad habilidad) {
        verificarEditable();
        if (habilidad != null && !etiquetas.contains(habilidad)) {
            etiquetas.add(habilidad);
        }
    }

    public void agregarFranja(FranjaDisponibilidad franja) {
        verificarEditable();
        if (franja != null) {
            franja.asignarPublicacion(this);
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
        verificarEditable();
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El titulo no puede quedar vacio");
        }
        this.titulo = titulo;
    }

    public TipoPublicacion getTipo() {
        return tipo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    /** "Cupo" de la seccion 4.2: suma del cupo de todas sus franjas. */
    public int getCupoTotal() {
        return franjas.stream().mapToInt(FranjaDisponibilidad::getCupoTotal).sum();
    }

    public int getCupoDisponible() {
        return franjas.stream().mapToInt(FranjaDisponibilidad::getCupoDisponible).sum();
    }

    public double getReputacionAutor() {
        return autor instanceof Perfil perfil ? perfil.getReputacionPromedio() : 0;
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

    public int getDuracionEstimadaMinutos() {
        return duracionEstimadaMinutos;
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
        return estadoNombre;
    }

    public int getContadorSolicitudes() {
        return contadorSolicitudes;
    }
}
