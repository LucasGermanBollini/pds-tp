package com.uade.skillswap.domain.model.state;

/** Terminal: solo consultar historial (seccion 4.5). */
public class EstadoContratacionRechazada extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "RECHAZADA";
    }
}
