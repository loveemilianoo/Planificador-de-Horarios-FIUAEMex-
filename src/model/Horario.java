package model;

import java.time.LocalTime;
import java.util.Objects;

public record Horario(DiaSemana dia, LocalTime inicio, LocalTime fin, String aula) {
    public Horario {
        Objects.requireNonNull(dia, "día es obligatorio");
        Objects.requireNonNull(inicio, "inicio es obligatorio");
        Objects.requireNonNull(fin, "fin es obligatorio");
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException(
                "inicio ("+inicio+") debe ser antes de fin ("+fin+")"
            );
        }
    }
}
