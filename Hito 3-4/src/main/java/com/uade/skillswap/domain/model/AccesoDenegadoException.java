package com.uade.skillswap.domain.model;

/**
 * Regla de la seccion 5: "Los controllers que modifican informacion deben
 * validar permisos a partir del userId recibido del servicio de identidad".
 * La validacion de QUIEN puede hacer cada accion vive en el dominio (GRASP
 * Information Expert: Publicacion conoce a su autor; Contratacion conoce a
 * su cliente y a su proveedor), y se expresa con esta excepcion propia para
 * que los controllers la traduzcan a un error uniforme ("ACCESO_DENEGADO")
 * distinto de un dato invalido o de una transicion invalida.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
