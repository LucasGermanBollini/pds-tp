package com.uade.skillswap.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Billetera de SkillCoins de un Perfil. El debito y el credito de una
 * operacion se hacen desde el caso de uso en una unica operacion logica
 * (seccion 5) y nunca dejan un saldo negativo. `titularUserId` es unico:
 * un perfil tiene como maximo una wallet (restriccion de unicidad, seccion 8).
 */
@Entity
@Table(name = "wallets_skillcoins")
public class WalletSkillCoins {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "titular_user_id", nullable = false, unique = true)
    private String titularUserId;

    @Embedded
    private SkillCoins saldo;

    protected WalletSkillCoins() {
        // JPA
    }

    public WalletSkillCoins(String id, String titularUserId, SkillCoins saldoInicial) {
        this.id = id;
        this.titularUserId = titularUserId;
        this.saldo = saldoInicial == null ? SkillCoins.CERO : saldoInicial;
    }

    public String getId() {
        return id;
    }

    public String getTitularUserId() {
        return titularUserId;
    }

    public SkillCoins getSaldo() {
        return saldo;
    }

    public boolean tieneSaldoSuficiente(SkillCoins monto) {
        return saldo.esMayorOIgual(monto);
    }

    public void debitar(SkillCoins monto) {
        if (!tieneSaldoSuficiente(monto)) {
            throw new IllegalStateException("Saldo insuficiente de SkillCoins");
        }
        this.saldo = this.saldo.restar(monto);
    }

    public void acreditar(SkillCoins monto) {
        this.saldo = this.saldo.sumar(monto);
    }
}
