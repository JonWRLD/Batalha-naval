package batalhanaval.domain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Jogador CPU com estratégias: RANDOM, HUNT, PARITY.
 * Sem nenhuma dependência de UI.
 */
public class CpuPlayer extends Jogador {

    private final String strategy;
    private final boolean parityPreference;
    private final Random rng;
    private final int n;

    private final boolean[][] tried;
    private final Deque<Coordenada> targets = new ArrayDeque<>();

    public CpuPlayer(String nome, Tabuleiro tabuleiro, Tabuleiro alvo,
                     String strategy, boolean parityPreference, Random rng) {
        super(nome, "cpu", tabuleiro, alvo);
        this.strategy = strategy;
        this.parityPreference = parityPreference;
        this.rng = rng;
        this.n = tabuleiro.getTamanho();
        this.tried = new boolean[n][n];
    }

    @Override
    public Coordenada escolherAlvo() {
        return switch (strategy) {
            case "HUNT"   -> huntShot();
            case "PARITY" -> parityShot();
            default       -> randomShot();
        };
    }

    @Override
    public void notificarResultado(ResultadoTiro resultado) {
        if (!resultado.isHit()) return;

        Coordenada c = resultado.getCoordenada();

        if (resultado.getTipo() == ResultadoTiro.Tipo.AFUNDOU) {
            // Navio afundado: descarta fila de alvos com certa probabilidade
            if (rng.nextInt(100) < 60) targets.clear();
            return;
        }

        // Acerto: enfileira vizinhos ortogonais
        int r = c.getRow();
        int col = c.getCol();
        enqueueIfValid(col + 1, r);
        enqueueIfValid(col - 1, r);
        enqueueIfValid(col, r + 1);
        enqueueIfValid(col, r - 1);
    }

    private Coordenada huntShot() {
        // Esgota fila de alvos antes de atirar aleatório
        while (!targets.isEmpty()) {
            Coordenada t = targets.pollFirst();
            if (!tried[t.getRow()][t.getCol()]) return markAndReturn(t);
        }
        return randomShot();
    }

    private Coordenada parityShot() {
        // Prefere células onde (col+row) % 2 == 0
        if (parityPreference) {
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    if (!tried[r][c] && (c + r) % 2 == 0)
                        return markAndReturn(new Coordenada(c, r));
                }
            }
        }
        return randomShot();
    }

    private Coordenada randomShot() {
        int tries = 0;
        while (tries++ < n * n * 4) {
            int c = rng.nextInt(n);
            int r = rng.nextInt(n);
            if (tried[r][c]) continue;
            // Aplica preferência de paridade quando habilitada
            if (parityPreference && (c + r) % 2 != 0 && rng.nextInt(100) >= 25) continue;
            return markAndReturn(new Coordenada(c, r));
        }
        // Fallback: primeira célula não tentada
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                if (!tried[r][c]) return markAndReturn(new Coordenada(c, r));
        return null;
    }

    private Coordenada markAndReturn(Coordenada c) {
        tried[c.getRow()][c.getCol()] = true;
        return c;
    }

    private void enqueueIfValid(int col, int row) {
        if (col >= 0 && col < n && row >= 0 && row < n && !tried[row][col])
            targets.addLast(new Coordenada(col, row));
    }
}
