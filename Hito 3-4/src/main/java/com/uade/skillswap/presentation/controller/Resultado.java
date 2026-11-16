package com.uade.skillswap.presentation.controller;

/**
 * Respuesta uniforme de todos los controllers (seccion 9: "manejar los
 * errores de aplicacion de forma uniforme"). O trae un valor, o trae un
 * codigo de error estable mas un mensaje legible para mostrar en pantalla.
 *
 * <p>Codigos: DATOS_INVALIDOS, ACCESO_DENEGADO, OPERACION_NO_PERMITIDA,
 * CONFLICTO_CONCURRENTE, NO_ENCONTRADO, ERROR_INTERNO.
 */
public record Resultado<T>(boolean exito, T valor, String codigoError, String mensaje) {

    public static <T> Resultado<T> ok(T valor) {
        return new Resultado<>(true, valor, null, null);
    }

    public static <T> Resultado<T> error(String codigo, String mensaje) {
        return new Resultado<>(false, null, codigo, mensaje);
    }

    /** Para quien prefiere excepciones (los adaptadores del Mock UI Kit, cuya interfaz devuelve valores). */
    public T valorOLanzar() {
        if (!exito) {
            throw new ErrorDeAplicacionException(codigoError, mensaje);
        }
        return valor;
    }
}
