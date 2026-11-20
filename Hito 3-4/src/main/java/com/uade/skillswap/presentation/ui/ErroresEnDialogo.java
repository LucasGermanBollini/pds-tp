package com.uade.skillswap.presentation.ui;

import ar.edu.uade.skillswap.controllers.UiControllers;
import com.uade.skillswap.presentation.controller.ErrorDeAplicacionException;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

/**
 * Seccion 10: "Detalle de publicacion: invocar solicitud y mostrar errores de
 * aplicacion". Las pantallas del kit no atrapan excepciones; en vez de
 * modificarlas, se envuelve cada controller en un proxy (Decorator
 * dinamico) que, si la operacion falla, muestra el mensaje en un dialogo y
 * corta la accion (relanza una excepcion "ya mostrada" que el manejador del
 * hilo de Swing ignora).
 */
public final class ErroresEnDialogo {

    /** Marca un error que ya se le mostro al usuario. */
    public static final class ErrorYaMostrado extends RuntimeException {
        ErrorYaMostrado(Throwable causa) {
            super(causa.getMessage(), causa, false, false);
        }
    }

    private ErroresEnDialogo() {
    }

    public static UiControllers envolver(UiControllers real, Supplier<Component> ventana) {
        return new UiControllers() {
            private final BusquedaController busqueda = proxy(BusquedaController.class, real.busqueda(), ventana);
            private final PublicacionController publicaciones = proxy(PublicacionController.class, real.publicaciones(), ventana);
            private final ContratacionController contrataciones = proxy(ContratacionController.class, real.contrataciones(), ventana);
            private final WalletController wallet = proxy(WalletController.class, real.wallet(), ventana);
            private final NotificacionController notificaciones = proxy(NotificacionController.class, real.notificaciones(), ventana);
            private final PerfilController perfiles = proxy(PerfilController.class, real.perfiles(), ventana);

            @Override public BusquedaController busqueda() { return busqueda; }
            @Override public PublicacionController publicaciones() { return publicaciones; }
            @Override public ContratacionController contrataciones() { return contrataciones; }
            @Override public WalletController wallet() { return wallet; }
            @Override public NotificacionController notificaciones() { return notificaciones; }
            @Override public PerfilController perfiles() { return perfiles; }
        };
    }

    /** Hace que el hilo de Swing no imprima el stack de un error que ya se mostro en un dialogo. */
    public static void silenciarErroresYaMostrados() {
        Thread.UncaughtExceptionHandler anterior = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((hilo, ex) -> {
            if (ex instanceof ErrorYaMostrado) {
                return;
            }
            if (anterior != null) {
                anterior.uncaughtException(hilo, ex);
            } else {
                ex.printStackTrace();
            }
        });
    }

    public static void mostrar(Component ventana, Throwable error) {
        String titulo = error instanceof ErrorDeAplicacionException e ? titulo(e.getCodigo()) : "Error";
        JOptionPane.showMessageDialog(ventana, error.getMessage(), titulo, JOptionPane.WARNING_MESSAGE);
    }

    private static String titulo(String codigo) {
        return switch (codigo == null ? "" : codigo) {
            case "ACCESO_DENEGADO" -> "No tenes permiso";
            case "DATOS_INVALIDOS" -> "Datos invalidos";
            case "OPERACION_NO_PERMITIDA" -> "Operacion no permitida";
            case "CONFLICTO_CONCURRENTE" -> "Otro usuario se adelanto";
            case "NO_ENCONTRADO" -> "No encontrado";
            default -> "Error";
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> tipo, T real, Supplier<Component> ventana) {
        InvocationHandler handler = (proxy, metodo, args) -> {
            try {
                return metodo.invoke(real, args);
            } catch (InvocationTargetException ex) {
                Throwable causa = ex.getCause();
                mostrar(ventana.get(), causa);
                throw new ErrorYaMostrado(causa);
            }
        };
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, handler);
    }
}
