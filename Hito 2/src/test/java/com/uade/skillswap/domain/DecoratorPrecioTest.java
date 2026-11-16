package com.uade.skillswap.domain;

import com.uade.skillswap.domain.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecoratorPrecioTest {

    @Test
    void elPrecioBaseSinPromocionesEsElMontoBase() {
        Cotizable precio = new PrecioPublicacion(SkillCoins.de(1000));
        assertEquals(SkillCoins.de(1000), precio.calcularPrecio());
    }

    @Test
    void lasPromocionesSonAcumulables() {
        Cotizable precio = new PrecioPublicacion(SkillCoins.de(1000));
        Cotizable conDescuentoPorcentual = new DescuentoPorcentual(precio, 10); // -100
        Cotizable conAmbas = new DescuentoMontoFijo(conDescuentoPorcentual, SkillCoins.de(200)); // -200

        assertEquals(SkillCoins.de(700), conAmbas.calcularPrecio());
    }

    @Test
    void lasPromocionesAcumuladasNuncaDanUnPrecioNegativo() {
        Cotizable precio = new PrecioPublicacion(SkillCoins.de(100));
        Cotizable conMuchoDescuento = new DescuentoMontoFijo(precio, SkillCoins.de(1000));

        assertEquals(SkillCoins.CERO, conMuchoDescuento.calcularPrecio());
    }

    @Test
    void skillCoinsNuncaPermiteValorNegativoAlConstruirse() {
        assertThrows(IllegalArgumentException.class, () -> new SkillCoins(-1));
    }

    @Test
    void debitarMasQueElSaldoLanzaYNoModificaNada() {
        WalletSkillCoins wallet = new WalletSkillCoins("w-1", "cli-1", SkillCoins.de(50));
        assertFalse(wallet.tieneSaldoSuficiente(SkillCoins.de(100)));
        assertThrows(IllegalStateException.class, () -> wallet.debitar(SkillCoins.de(100)));
        assertEquals(SkillCoins.de(50), wallet.getSaldo(), "el saldo no debe cambiar si el debito fallo");
    }
}
