package com.uade.skillswap.presentation;

import com.uade.skillswap.application.excepcion.ConflictoConcurrenteException;
import com.uade.skillswap.domain.model.AccesoDenegadoException;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.presentation.controller.ControllerBase;
import com.uade.skillswap.presentation.controller.ErrorDeAplicacionException;
import com.uade.skillswap.presentation.controller.Resultado;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Seccion 9: "manejar los errores de aplicacion de forma uniforme". */
class ManejoDeErroresTest {

    /** Un controller minimo que solo expone el manejo comun. */
    static class ControllerDePrueba extends ControllerBase {
        ControllerDePrueba() {
            super(userId -> {
                if (userId == null || userId.isBlank()) {
                    throw new AccesoDenegadoException("usuario desconocido");
                }
            });
        }

        <T> Resultado<T> probar(Supplier<T> s) {
            return consultar(s);
        }

        <T> Resultado<T> probarComo(String userId, Supplier<T> s) {
            return modificar(userId, s);
        }
    }

    private final ControllerDePrueba controller = new ControllerDePrueba();

    private String codigo(RuntimeException ex) {
        return controller.probar(() -> {
            throw ex;
        }).codigoError();
    }

    @Test
    void cadaTipoDeErrorTieneSuCodigo() {
        assertEquals("ACCESO_DENEGADO", codigo(new AccesoDenegadoException("no")));
        assertEquals("CONFLICTO_CONCURRENTE", codigo(new ConflictoConcurrenteException("otro gano", null)));
        assertEquals("DATOS_INVALIDOS", codigo(new IllegalArgumentException("falta el titulo")));
        assertEquals("NO_ENCONTRADO", codigo(new IllegalArgumentException("Publicacion inexistente: x")));
        assertEquals("OPERACION_NO_PERMITIDA", codigo(new IllegalStateException("No se puede pagar desde PENDIENTE")));
        Resultado<Object> inesperado = controller.probar(() -> {
            throw new UnsupportedOperationException("detalle interno");
        });
        assertEquals("ERROR_INTERNO", inesperado.codigoError());
        assertFalse(inesperado.mensaje().contains("detalle interno"), "no se filtran detalles internos a la interfaz");
    }

    @Test
    void modificarValidaElUsuarioAntesDeEjecutar() {
        boolean[] ejecuto = {false};
        Resultado<Boolean> r = controller.probarComo("", () -> ejecuto[0] = true);
        assertEquals("ACCESO_DENEGADO", r.codigoError());
        assertFalse(ejecuto[0], "si la identidad falla, el caso de uso ni se invoca");
        assertTrue(controller.probarComo("user-1", () -> true).valor());
    }

    @Test
    void resultadoOkYValorOLanzar() {
        assertEquals(5, Resultado.ok(5).valorOLanzar());
        ErrorDeAplicacionException ex = assertThrows(ErrorDeAplicacionException.class,
                () -> Resultado.error("DATOS_INVALIDOS", "mal").valorOLanzar());
        assertEquals("DATOS_INVALIDOS", ex.getCodigo());
        assertEquals("mal", ex.getMessage());
    }

    @Test
    void paginaCalculaTotalesYNormalizaElTamanio() {
        Pagina<Integer> p = Pagina.de(List.of(1, 2, 3, 4, 5), 1, 2);
        assertEquals(List.of(3, 4), p.contenido());
        assertEquals(3, p.totalPaginas());
        assertTrue(p.tieneAnterior());
        assertTrue(p.tieneSiguiente());
        assertEquals(List.of(), Pagina.de(List.of(1, 2), 5, 2).contenido(), "una pagina fuera de rango viene vacia");
        assertEquals(Pagina.TAMANIO_POR_DEFECTO, Pagina.de(List.of(1), 0, 0).tamanio());
        assertEquals(Pagina.TAMANIO_MAXIMO, Pagina.de(List.of(1), 0, 10_000).tamanio());
        assertEquals(List.of("3", "4"), p.map(String::valueOf).contenido());
    }
}
