package com.uade.skillswap.application.excepcion;

/**
 * Seccion 8: optimistic locking. Cuando dos transacciones modifican la misma
 * fila (por ejemplo, dos aceptaciones que quieren el ultimo cupo de una
 * franja), la segunda falla. Infraestructura traduce la excepcion propia de
 * JPA a esta, asi Aplicacion y Presentacion no dependen de jakarta.persistence.
 */
public class ConflictoConcurrenteException extends IllegalStateException {

    public ConflictoConcurrenteException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
