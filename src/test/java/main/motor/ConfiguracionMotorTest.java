package main.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfiguracionMotorTest {

    private static final LocalTime CORTE = LocalTime.of(13, 0);

    @Test
    @DisplayName("Los valores por defecto son los del Acta: 2 a 4 opciones y corte a las 13:00")
    void valoresPorDefecto() {
        ConfiguracionMotor c = ConfiguracionMotor.porDefecto();
        assertEquals(2, c.minOpciones());
        assertEquals(4, c.maxOpciones());
        assertEquals(5000, c.maxCandidatas());
        assertEquals(CORTE, c.horaCorteTurno());
    }

    @Test
    @DisplayName("Rechaza minOpciones menor a 1")
    void rechazaMinInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionMotor(0, 4, 100, CORTE));
    }

    @Test
    @DisplayName("Rechaza maxOpciones menor que minOpciones")
    void rechazaMaxMenorQueMin() {
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionMotor(3, 2, 100, CORTE));
    }

    @Test
    @DisplayName("Rechaza maxCandidatas menor que maxOpciones")
    void rechazaCandidatasInsuficientes() {
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionMotor(2, 4, 3, CORTE));
    }

    @Test
    @DisplayName("Rechaza hora de corte nula")
    void rechazaCorteNulo() {
        assertThrows(NullPointerException.class, () -> new ConfiguracionMotor(2, 4, 100, null));
    }

    @Test
    @DisplayName("Acepta límites iguales (min = max = candidatas)")
    void aceptaLimitesIguales() {
        ConfiguracionMotor c = new ConfiguracionMotor(1, 1, 1, CORTE);
        assertEquals(1, c.maxOpciones());
    }
}
