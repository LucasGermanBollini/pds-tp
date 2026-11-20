package com.uade.skillswap.domain.model;

import java.time.LocalDateTime;

public final class MovimientoCreditos {

    private final String id;
    private final String walletId;
    private final TipoMovimientoCreditos tipo;
    private final SkillCoins monto;
    private final String motivo;
    private final String contratacionId;
    private final LocalDateTime fecha;

    public MovimientoCreditos(String id, String walletId, TipoMovimientoCreditos tipo, SkillCoins monto,
                               String motivo, String contratacionId, LocalDateTime fecha) {
        this.id = id;
        this.walletId = walletId;
        this.tipo = tipo;
        this.monto = monto;
        this.motivo = motivo;
        this.contratacionId = contratacionId;
        this.fecha = fecha;
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
}
