package com.uade.skillswap.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Registro de auditoria del movimiento; referencia la wallet por id, sin relacion JPA (estilo log). */
@Entity
@Table(name = "movimientos_creditos")
public class MovimientoCreditos {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "wallet_id", nullable = false, length = 36)
    private String walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimientoCreditos tipo;

    @Embedded
    private SkillCoins monto;

    private String motivo;

    @Column(name = "contratacion_id", length = 36)
    private String contratacionId;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Saldo de la wallet inmediatamente despues de este movimiento (estilo libro mayor, para listarMovimientos de la UI). */
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "saldo_posterior"))
    private SkillCoins saldoPosterior;

    protected MovimientoCreditos() {
        // JPA
    }

    public MovimientoCreditos(String id, String walletId, TipoMovimientoCreditos tipo, SkillCoins monto,
                               String motivo, String contratacionId, LocalDateTime fecha, SkillCoins saldoPosterior) {
        this.id = id;
        this.walletId = walletId;
        this.tipo = tipo;
        this.monto = monto;
        this.motivo = motivo;
        this.contratacionId = contratacionId;
        this.fecha = fecha;
        this.saldoPosterior = saldoPosterior;
    }

    public String getId() {
        return id;
    }

    public String getWalletId() {
        return walletId;
    }

    public TipoMovimientoCreditos getTipo() {
        return tipo;
    }

    public SkillCoins getMonto() {
        return monto;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getContratacionId() {
        return contratacionId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public SkillCoins getSaldoPosterior() {
        return saldoPosterior;
    }
}
