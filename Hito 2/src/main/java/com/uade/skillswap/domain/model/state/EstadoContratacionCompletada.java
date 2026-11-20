package com.uade.skillswap.domain.model.state;

/** Terminal: habilita calificar y consultar comprobante (seccion 4.5), sin mas transiciones. */
public class EstadoContratacionCompletada extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "COMPLETADA";
    }
}
