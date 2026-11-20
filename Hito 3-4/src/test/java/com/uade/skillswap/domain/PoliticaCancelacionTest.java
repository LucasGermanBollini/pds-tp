package com.uade.skillswap.domain;

import com.uade.skillswap.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaCancelacionTest {

    private Contratacion contratacionConFranjaEn(long horasDesdeAhora) {
        Perfil autor = new Perfil("prof-1", "Ada");
        Publicacion publicacion = new Publicacion("pub-1", autor, "Mentoria");
        CategoriaHabilidad categoria = new CategoriaHabilidad("cat-1", "Carrera");
        FranjaDisponibilidad franja = new FranjaDisponibilidad("f-1", LocalDateTime.now().plusHours(horasDesdeAhora),
                LocalDateTime.now().plusHours(horasDesdeAhora + 1), 2);
        publicacion.completarDatos("desc", categoria, Modalidad.REMOTA, 60, SkillCoins.de(1000));
        publicacion.agregarFranja(franja);
        publicacion.publicar("prof-1");
        Perfil cliente = new Perfil("cli-1", "Beto");
        return new Contratacion("c-1", cliente, publicacion, franja, "hola", "practicar");
    }

    @Test
    void laPoliticaEstandarReintegraTodoConMasDe48HorasDeAnticipacion() {
        Contratacion contratacion = contratacionConFranjaEn(72);
        SkillCoins reintegro = new PoliticaCancelacionEstandar()
                .calcularReintegro(SkillCoins.de(1000), contratacion, LocalDateTime.now());
        assertEquals(SkillCoins.de(1000), reintegro);
    }

    @Test
    void laPoliticaEstandarNoReintegraNadaConMenosDe24Horas() {
        Contratacion contratacion = contratacionConFranjaEn(10);
        SkillCoins reintegro = new PoliticaCancelacionEstandar()
                .calcularReintegro(SkillCoins.de(1000), contratacion, LocalDateTime.now());
        assertEquals(SkillCoins.CERO, reintegro);
    }

    @Test
    void laPoliticaFlexibleReintegraMasQueLaEstandarConPocaAnticipacion() {
        // 10 horas de anticipacion: Estandar da 0%, Flexible da 50%.
        Contratacion contratacion = contratacionConFranjaEn(10);
        SkillCoins reintegroEstandar = new PoliticaCancelacionEstandar()
                .calcularReintegro(SkillCoins.de(1000), contratacion, LocalDateTime.now());
        SkillCoins reintegroFlexible = new PoliticaCancelacionFlexible()
                .calcularReintegro(SkillCoins.de(1000), contratacion, LocalDateTime.now());

        assertEquals(SkillCoins.CERO, reintegroEstandar);
        assertEquals(SkillCoins.de(500), reintegroFlexible);
    }
}
