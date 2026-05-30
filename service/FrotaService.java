package batalhanaval.service;

import batalhanaval.config.GameConfig;
import batalhanaval.domain.*;

import java.util.Random;
import java.util.function.BiFunction;

/**
 * Serviço que posiciona a frota num tabuleiro.
 * Não imprime nada; usa callbacks para obter entradas do usuário.
 */
public class FrotaService {

    private final GameConfig config;

    public FrotaService(GameConfig config) {
        this.config = config;
    }

    /**
     * Posiciona a frota automaticamente usando o RNG.
     * @return true se conseguiu posicionar todos os navios.
     */
    public boolean posicionarAutomatico(Tabuleiro tabuleiro, Random rng) {
        int[] tamanhos = config.getFleetSizes();
        String[] nomes = config.getFleetNames();
        String adj = config.getAdjacencyRule();
        int n = tabuleiro.getTamanho();

        for (int sid = 0; sid < tamanhos.length; sid++) {
            Navio navio = new Navio(sid, nomes[sid], tamanhos[sid]);
            boolean ok = false;
            for (int t = 0; t < 5000 && !ok; t++) {
                boolean horiz = rng.nextInt(2) == 0;
                int col = rng.nextInt(n);
                int row = rng.nextInt(n);
                ok = tabuleiro.posicionarNavio(navio, new Coordenada(col, row), horiz, adj);
            }
            if (!ok) return false;
        }
        return true;
    }

    /**
     * Posiciona manualmente, usando um callback para cada navio.
     * O callback recebe (índice do navio, mensagem de erro anterior) e retorna
     * um array [Coordenada, Boolean horiz], ou null para cancelar.
     *
     * Na prática é a UI que implementa o callback.
     */
    public boolean posicionarManual(Tabuleiro tabuleiro,
            BiFunction<Integer, String, PlacementInput> callback) {

        int[] tamanhos = config.getFleetSizes();
        String[] nomes = config.getFleetNames();
        String adj = config.getAdjacencyRule();

        for (int sid = 0; sid < tamanhos.length; sid++) {
            Navio navio = new Navio(sid, nomes[sid], tamanhos[sid]);
            String erro = null;
            boolean colocado = false;
            while (!colocado) {
                PlacementInput input = callback.apply(sid, erro);
                if (input == null) return false; // cancelado

                boolean ok = tabuleiro.posicionarNavio(navio, input.coordenada(),
                        input.horizontal(), adj);
                if (!ok) {
                    erro = "Posição inválida (fora do tabuleiro, colisão ou adjacência). Tente novamente.";
                } else {
                    colocado = true;
                }
            }
        }
        return true;
    }

    /** DTO simples para retorno do callback manual */
    public record PlacementInput(Coordenada coordenada, boolean horizontal) {}
}
