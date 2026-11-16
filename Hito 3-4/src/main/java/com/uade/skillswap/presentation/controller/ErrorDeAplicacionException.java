package com.uade.skillswap.presentation.controller;

/** Un Resultado de error convertido en excepcion (con su codigo), para mostrarlo en la interfaz. */
public class ErrorDeAplicacionException extends RuntimeException {

    private final String codigo;

    public ErrorDeAplicacionException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
