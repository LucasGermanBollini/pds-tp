package com.uade.skillswap.domain.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "perfiles")
public class Perfil implements Publicador {

    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "nombre_visible", nullable = false)
    private String nombreVisible;

    /** PROFESIONAL publica ofertas; ORGANIZACION publica desafios (seccion 4.2). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_perfil", nullable = false)
    private TipoPerfil tipo = TipoPerfil.PROFESIONAL;

    @Column(length = 2000)
    private String biografia;

    @Column(name = "zona_horaria")
    private String zonaHoraria;

    /** Tagline corta (ej. "Software Engineer & Mentor"); pide Models.Perfil.rol del Mock UI Kit. */
    @Column(name = "rol_descripcion")
    private String rolDescripcion;

    /** Seccion 4.6: "Cada usuario elige email, push o notificacion interna". */
    @Enumerated(EnumType.STRING)
    @Column(name = "canal_preferido", nullable = false)
    private TipoCanalNotificacion canalPreferido = TipoCanalNotificacion.INTERNA;

    /** EAGER por el mismo motivo que Publicacion.franjas/etiquetas: agregarHabilidad() se llama sobre entidades ya detached. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "perfil_habilidades",
            joinColumns = @JoinColumn(name = "perfil_id"),
            inverseJoinColumns = @JoinColumn(name = "habilidad_id"))
    private List<Habilidad> habilidades = new ArrayList<>();

    /**
     * Seccion 4.1: "intereses". Son habilidades que el usuario quiere
     * aprender (no las que ya ofrece); la estrategia de recomendacion por
     * afinidad las tiene en cuenta.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "perfil_intereses",
            joinColumns = @JoinColumn(name = "perfil_id"),
            inverseJoinColumns = @JoinColumn(name = "habilidad_id"))
    private List<Habilidad> intereses = new ArrayList<>();

    /** Seccion 4.1: "disponibilidad". Coleccion de value objects (no tienen identidad propia). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_disponibilidad", joinColumns = @JoinColumn(name = "perfil_id"))
    private List<DisponibilidadSemanal> disponibilidad = new ArrayList<>();

    @Column(name = "reputacion_promedio", nullable = false)
    private double reputacionPromedio;

    @Column(name = "cantidad_calificaciones", nullable = false)
    private int cantidadCalificaciones;

    private boolean activo = true;

    protected Perfil() {
        // JPA
    }

    public Perfil(String id, String nombreVisible) {
        this(id, nombreVisible, TipoPerfil.PROFESIONAL);
    }

    public Perfil(String id, String nombreVisible, TipoPerfil tipo) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El userId del perfil es obligatorio");
        }
        if (nombreVisible == null || nombreVisible.isBlank()) {
            throw new IllegalArgumentException("El nombre visible es obligatorio");
        }
        this.id = id;
        this.nombreVisible = nombreVisible;
        this.tipo = tipo == null ? TipoPerfil.PROFESIONAL : tipo;
    }

    @Override
    public String obtenerId() {
        return id;
    }

    @Override
    public String obtenerNombreVisible() {
        return nombreVisible;
    }

    @Override
    public boolean estaActivo() {
        return activo;
    }

    @Override
    public boolean puedePublicar(TipoPublicacion tipoPublicacion) {
        return activo && tipo.puedePublicar(tipoPublicacion);
    }

    /** Un perfil dado de baja no puede operar (publicar, solicitar, aceptar...). */
    public void verificarActivo() {
        if (!activo) {
            throw new IllegalStateException("El perfil " + id + " esta dado de baja");
        }
    }

    public void actualizarDatos(String nombreVisible, String biografia, String zonaHoraria, String rolDescripcion) {
        if (nombreVisible != null && !nombreVisible.isBlank()) {
            this.nombreVisible = nombreVisible;
        }
        this.biografia = biografia;
        this.zonaHoraria = zonaHoraria;
        this.rolDescripcion = rolDescripcion;
    }

    public void agregarHabilidad(Habilidad habilidad) {
        if (habilidad != null && !habilidades.contains(habilidad)) {
            habilidades.add(habilidad);
        }
    }

    public List<Habilidad> getHabilidades() {
        return List.copyOf(habilidades);
    }

    public void agregarInteres(Habilidad habilidad) {
        if (habilidad != null && !intereses.contains(habilidad)) {
            intereses.add(habilidad);
        }
    }

    public List<Habilidad> getIntereses() {
        return List.copyOf(intereses);
    }

    /** Reemplaza la disponibilidad completa; no admite rangos superpuestos el mismo dia. */
    public void definirDisponibilidad(List<DisponibilidadSemanal> nuevaDisponibilidad) {
        List<DisponibilidadSemanal> rangos = nuevaDisponibilidad == null ? List.of()
                : nuevaDisponibilidad.stream().filter(Objects::nonNull).toList();
        for (int i = 0; i < rangos.size(); i++) {
            for (int j = i + 1; j < rangos.size(); j++) {
                if (rangos.get(i).seSuperponeCon(rangos.get(j))) {
                    throw new IllegalArgumentException("La disponibilidad tiene rangos superpuestos: "
                            + rangos.get(i) + " y " + rangos.get(j));
                }
            }
        }
        disponibilidad.clear();
        disponibilidad.addAll(rangos);
    }

    public List<DisponibilidadSemanal> getDisponibilidad() {
        return List.copyOf(disponibilidad);
    }

    public TipoPerfil getTipo() {
        return tipo;
    }

    /**
     * Seccion 5: "La baja de un perfil no elimina operaciones historicas: se
     * realiza baja logica". La fila queda (y con ella sus contrataciones,
     * movimientos y calificaciones); solo deja de poder operar.
     */
    public void darBajaLogica() {
        this.activo = false;
    }

    public boolean isActivo() {
        return activo;
    }

    /**
     * La reputacion se recalcula a partir de calificaciones verificadas; nunca
     * se expone un setter directo (regla de negocio, seccion 4.1).
     */
    public void recalcularReputacion(List<Calificacion> calificacionesRecibidas) {
        if (calificacionesRecibidas == null || calificacionesRecibidas.isEmpty()) {
            this.reputacionPromedio = 0;
            this.cantidadCalificaciones = 0;
            return;
        }
        double suma = calificacionesRecibidas.stream().mapToInt(Calificacion::getPuntaje).sum();
        this.reputacionPromedio = suma / calificacionesRecibidas.size();
        this.cantidadCalificaciones = calificacionesRecibidas.size();
    }

    public double getReputacionPromedio() {
        return reputacionPromedio;
    }

    public int getCantidadCalificaciones() {
        return cantidadCalificaciones;
    }

    public String getBiografia() {
        return biografia;
    }

    public String getZonaHoraria() {
        return zonaHoraria;
    }

    public String getRolDescripcion() {
        return rolDescripcion;
    }

    public TipoCanalNotificacion getCanalPreferido() {
        return canalPreferido;
    }

    public void elegirCanalPreferido(TipoCanalNotificacion canal) {
        if (canal != null) {
            this.canalPreferido = canal;
        }
    }
}
