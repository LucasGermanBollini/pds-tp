package com.uade.skillswap.domain.model;

import java.util.Objects;

/**
 * Value object para montos en SkillCoins. Evita la primitive obsession
 * (Clase 3) de mezclar precios/saldos con long o double sueltos.
 */
public final class SkillCoins {

    public static final SkillCoins CERO = new SkillCoins(0);

    private final long valor;

    public SkillCoins(long valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("SkillCoins no puede ser negativo: " + valor);
        }
        this.valor = valor;
    }

    public static SkillCoins de(long valor) {
        return new SkillCoins(valor);
    }

    public long valor() {
        return valor;
    }

    public SkillCoins sumar(SkillCoins otro) {
        return new SkillCoins(this.valor + otro.valor);
    }

    /** Resta sin permitir resultado negativo (se trunca en 0), util para promociones. */
    public SkillCoins restarSinNegativo(SkillCoins otro) {
        long resultado = this.valor - otro.valor;
        return new SkillCoins(Math.max(0, resultado));
    }

    /** Resta estricta: lanza si el resultado seria negativo (uso en debitos de saldo). */
    public SkillCoins restar(SkillCoins otro) {
        long resultado = this.valor - otro.valor;
        if (resultado < 0) {
            throw new IllegalArgumentException("La resta produce un SkillCoins negativo");
        }
        return new SkillCoins(resultado);
    }

    public boolean esMayorOIgual(SkillCoins otro) {
        return this.valor >= otro.valor;
    }

    public boolean esNegativo() {
        return valor < 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SkillCoins sc && sc.valor == this.valor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor + " SkillCoins";
    }
}
