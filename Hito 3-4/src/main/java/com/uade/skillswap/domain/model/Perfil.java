package com.uade.skillswap.domain.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "perfiles")
public class Perfil implements Publicador {

    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "nombre_visible", nullable = false)
    private String nombreVisible;

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

    @Column(name = "reputacion_promedio", nullable = false)
    private double reputacionPromedio;

    @Column(name = "cantidad_calificaciones", nullable = false)
    private int cantidadCalificaciones;

    private boolean activo = true;

    protected Perfil() {
        // JPA
    }

    public Perfil(String id, String nombreVisible) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El userId del perfil es obligatorio");
        }
        if (nombreVisible == null || nombreVisible.isBlank()) {
            throw new IllegalArgumentException("El nombre visible es obligatorio");
        }
        this.id = id;
        this.nombreVisible = nombreVisible;
    }

    @Override
    public String obtenerId() {
        return id;
    }

    @Override
    public String obtenerNombreVisible() {
        return nombreVisible;
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
