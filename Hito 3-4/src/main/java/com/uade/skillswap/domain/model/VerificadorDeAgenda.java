package com.uade.skillswap.domain.model;

import java.util.List;

/**
 * Seccion 5: "No puede reservarse una franja ocupada". El cupo lo cuida cada
 * FranjaDisponibilidad; esta clase cuida la agenda del CLIENTE: no puede
 * tener dos sesiones activas que se pisen en el tiempo, ni pedir dos veces
 * la misma franja.
 *
 * <p>GRASP Pure Fabrication: la regla involucra varias contrataciones a la
 * vez, asi que no es responsabilidad natural de ninguna de ellas.
 */
public final class VerificadorDeAgenda {

    private VerificadorDeAgenda() {
    }

    /**
     * @param contratacionesDelCliente contrataciones donde participa el cliente
     * @param contratacionAExcluir     id a ignorar (la propia, al reprogramar); puede ser null
     */
    public static void verificarQuePuedeReservar(String clienteUserId, FranjaDisponibilidad franja,
                                                  List<Contratacion> contratacionesDelCliente,
                                                  String contratacionAExcluir) {
        for (Contratacion otra : contratacionesDelCliente) {
            if (!otra.estaActiva() || !otra.esCliente(clienteUserId) || otra.getId().equals(contratacionAExcluir)) {
                continue;
            }
            if (otra.getFranjaElegida().equals(franja)) {
                throw new IllegalStateException("Ya tenes una solicitud activa para esa misma franja");
            }
            if (otra.getFranjaElegida().seSuperponeCon(franja)) {
                throw new IllegalStateException("La franja esta ocupada en tu agenda: ya tenes '"
                        + otra.getPublicacion().getTitulo() + "' en ese horario");
            }
        }
    }
}
