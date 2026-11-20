package com.uade.skillswap.domain.model.state;

/** Terminal: finalizada, no puede reabrirse (seccion 4.2). No sobrescribe nada de la base. */
public class EstadoPublicacionCerrada extends EstadoPublicacionBase {

    @Override
    public String getNombre() {
        return "CERRADA";
    }
}
