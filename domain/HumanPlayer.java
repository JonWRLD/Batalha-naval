package batalhanaval.domain;

import java.util.function.Function;

/**
 * Jogador humano. A UI fornece um callback que lê a coordenada do terminal.
 * O domínio nunca usa Scanner diretamente.
 */
public class HumanPlayer extends Jogador {

    // Recebe o tabuleiro alvo para validar "já atirado" e retorna a coordenada escolhida
    private final Function<Tabuleiro, Coordenada> inputCallback;

    public HumanPlayer(String nome, Tabuleiro tabuleiro, Tabuleiro alvo,
                       Function<Tabuleiro, Coordenada> inputCallback) {
        super(nome, "humano", tabuleiro, alvo);
        this.inputCallback = inputCallback;
    }

    @Override
    public Coordenada escolherAlvo() {
        return inputCallback.apply(getTabuleirAlvo());
    }
}
