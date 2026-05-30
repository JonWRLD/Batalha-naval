package batalhanaval.domain;

/**
 * Registro imutável de uma jogada (para log e persistência).
 */
public record Jogada(
        int turno,
        String jogador,
        String coordenada,
        String resultado
) {}
