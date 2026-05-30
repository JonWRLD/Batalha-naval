package batalhanaval.domain;

/**
 * Abstração de jogador. Implementações: HumanPlayer e CpuPlayer.
 */
public abstract class Jogador {

    private final String nome;
    private final String tipo; // "humano" ou "cpu"
    private final Tabuleiro tabuleiro;      // tabuleiro próprio (navios aqui)
    private final Tabuleiro tabuleirAlvo;   // tabuleiro do adversário (para atirar)

    protected Jogador(String nome, String tipo, Tabuleiro tabuleiro, Tabuleiro alvo) {
        this.nome = nome;
        this.tipo = tipo;
        this.tabuleiro = tabuleiro;
        this.tabuleirAlvo = alvo;
    }

    public String getNome()              { return nome; }
    public String getTipo()              { return tipo; }
    public Tabuleiro getTabuleiro()      { return tabuleiro; }
    public Tabuleiro getTabuleirAlvo()   { return tabuleirAlvo; }

    /** Decide a próxima coordenada para atirar (pode bloquear no terminal ou calcular). */
    public abstract Coordenada escolherAlvo();

    /** Informa o resultado do último tiro (para CPU atualizar estado interno). */
    public void notificarResultado(ResultadoTiro resultado) {}

    public boolean perdeu() { return tabuleiro.todosAfundados(); }
}
