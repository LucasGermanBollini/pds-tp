package com.uade.skillswap.application.dto;

import java.util.List;

/**
 * Seccion 4.1: "Consultar el perfil publico con reputacion, publicaciones
 * activas e historial resumido". Lo arma PerfilPublicoFacade juntando datos
 * de cuatro repositorios distintos.
 */
public record PerfilPublicoDTO(String id, String nombre, String tipo, String rol, String biografia,
                               String zonaHoraria, String canalPreferido, boolean activo,
                               List<String> habilidades, List<String> intereses, List<String> disponibilidad,
                               double reputacion, int cantidadCalificaciones,
                               List<PublicacionDTO> publicacionesActivas,
                               HistorialResumido historial) {

    /** Resumen de la actividad: cuantas sesiones dio/tomo y sus ultimas calificaciones recibidas. */
    public record HistorialResumido(int sesionesCompletadasComoProveedor, int sesionesCompletadasComoCliente,
                                    int contratacionesCanceladas, List<CalificacionDTO> ultimasCalificaciones) {

        public int totalCompletadas() {
            return sesionesCompletadasComoProveedor + sesionesCompletadasComoCliente;
        }
    }
}
