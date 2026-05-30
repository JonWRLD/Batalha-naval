package batalhanaval.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tabuleiro NxN.
 * Responsável por: posicionamento de navios, aplicação de tiros e consulta de estado.
 * Nunca imprime nada.
 */
public class Tabuleiro {

    // Símbolos das células
    public static final char VAZIO    = '.';
    public static final char NAVIO    = 'S';
    public static final char ACERTO   = 'X';
    public static final char AGUA     = 'o';

    private final int tamanho;
    private final char[][] grade;           // estado visual
    private final int[][] idNavioPorCelula; // -1 = vazio
    private final List<Navio> navios;
    private final boolean[][] atirado;

    public Tabuleiro(int tamanho) {
        this.tamanho = tamanho;
        this.grade = new char[tamanho][tamanho];
        this.idNavioPorCelula = new int[tamanho][tamanho];
        this.navios = new ArrayList<>();
        this.atirado = new boolean[tamanho][tamanho];

        for (int r = 0; r < tamanho; r++) {
            for (int c = 0; c < tamanho; c++) {
                grade[r][c] = VAZIO;
                idNavioPorCelula[r][c] = -1;
            }
        }
    }

    public int getTamanho() { return tamanho; }

    /**
     * Posiciona um navio. Retorna false se não cabe ou colide
     * (respeitando a regra de adjacência informada).
     */
    public boolean posicionarNavio(Navio navio, Coordenada inicio,
                                   boolean horizontal, String adjacencyRule) {
        int len = navio.getTamanho();
        int col = inicio.getCol();
        int row = inicio.getRow();

        if (horizontal && col + len > tamanho) return false;
        if (!horizontal && row + len > tamanho) return false;

        // Coleta células que o navio vai ocupar
        List<Coordenada> novasCelulas = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            int c = horizontal ? col + i : col;
            int r = horizontal ? row : row + i;
            novasCelulas.add(new Coordenada(c, r));
        }

        // Verifica colisão e adjacência para cada célula candidata
        for (Coordenada nova : novasCelulas) {
            if (grade[nova.getRow()][nova.getCol()] != VAZIO) return false;

            if (!adjacencyRule.equals("NONE")) {
                boolean ortho     = checkAdjacency(nova, false);
                boolean diagonal  = adjacencyRule.equals("ORTHO_DIAG") && checkAdjacency(nova, true);
                if (ortho || diagonal) return false;
            }
        }

        // Efetua o posicionamento
        for (Coordenada c : novasCelulas) {
            grade[c.getRow()][c.getCol()] = NAVIO;
            idNavioPorCelula[c.getRow()][c.getCol()] = navio.getId();
            navio.adicionarCelula(c);
        }
        navios.add(navio);
        return true;
    }

    /**
     * Verifica se alguma célula adjacente (ortogonal ou diagonal) já está ocupada,
     * ignorando as células do próprio navio que está sendo posicionado.
     */
    private boolean checkAdjacency(Coordenada nova, boolean includeDiagonals) {
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};
        int[] drDiag = {-1, -1, 1, 1};
        int[] dcDiag = {-1, 1, -1, 1};

        int[] drs = includeDiagonals ? drDiag : dr;
        int[] dcs = includeDiagonals ? dcDiag : dc;

        for (int i = 0; i < drs.length; i++) {
            int nr = nova.getRow() + drs[i];
            int nc = nova.getCol() + dcs[i];
            if (nr >= 0 && nr < tamanho && nc >= 0 && nc < tamanho) {
                if (grade[nr][nc] == NAVIO) return true;
            }
        }
        return false;
    }

    /** Aplica um tiro. Retorna o resultado sem imprimir nada. */
    public ResultadoTiro atirar(Coordenada c) {
        int r = c.getRow();
        int col = c.getCol();

        if (atirado[r][col]) return ResultadoTiro.jaAtirado(c);
        atirado[r][col] = true;

        if (grade[r][col] == NAVIO) {
            grade[r][col] = ACERTO;
            int id = idNavioPorCelula[r][col];
            Navio navio = navios.stream().filter(n -> n.getId() == id).findFirst().orElse(null);
            if (navio != null && navio.registrarAcerto()) {
                return ResultadoTiro.afundou(c, navio);
            }
            return ResultadoTiro.acerto(c);
        }

        grade[r][col] = AGUA;
        return ResultadoTiro.agua(c);
    }

    /** Retorna true se todos os navios foram afundados. */
    public boolean todosAfundados() {
        return navios.stream().allMatch(Navio::estaAfundado);
    }

    /** Número de navios ainda vivos. */
    public long naviosVivos() {
        return navios.stream().filter(n -> !n.estaAfundado()).count();
    }

    public char getCelula(int row, int col) { return grade[row][col]; }
    public boolean foiAtirado(int row, int col) { return atirado[row][col]; }
    public List<Navio> getNavios() { return Collections.unmodifiableList(navios); }

    /** Retorna uma cópia da grade (para a UI). */
    public char[][] getGrade() {
        char[][] copia = new char[tamanho][tamanho];
        for (int r = 0; r < tamanho; r++)
            System.arraycopy(grade[r], 0, copia[r], 0, tamanho);
        return copia;
    }
}
