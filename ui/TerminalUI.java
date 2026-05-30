package batalhanaval.ui;

import batalhanaval.config.GameConfig;
import batalhanaval.domain.*;
import batalhanaval.persistence.GameRepository;
import batalhanaval.service.FrotaService;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Interface do terminal. Responsável por: mostrar menus e tabuleiros,
 * ler entradas e apresentar resultados. Nunca contém lógica de domínio.
 */
public class TerminalUI {

    private final GameConfig config;
    private final Scanner sc;

    public TerminalUI(GameConfig config, Scanner sc) {
        this.config = config;
        this.sc = sc;
    }

    // ====== Menus iniciais ======

    public void mostrarBemVindo() {
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║      BATALHA NAVAL  v" + config.getVersion() + "       ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println();
    }

    public String lerMenuInicial() {
        System.out.println("O que deseja fazer?");
        System.out.println("  [1] Nova partida");
        System.out.println("  [2] Listar histórico de partidas");
        System.out.println("  [3] Replay de uma partida");
        System.out.println("  [X] Sair");
        System.out.print("> ");
        return sc.nextLine().trim().toUpperCase(Locale.ROOT);
    }

    // ====== Posicionamento ======

    public boolean perguntarPosicionamentoManual() {
        System.out.print("\nDeseja posicionar sua frota manualmente? (s/N): ");
        String resp = sc.nextLine().trim().toLowerCase(Locale.ROOT);
        return resp.equals("s") || resp.equals("sim");
    }

    /**
     * Callback para posicionamento manual de um navio.
     * Retorna PlacementInput ou null se o usuário digitar "auto".
     */
    public FrotaService.PlacementInput pedirPosicionamento(int sid, String erro) {
        int[] tamanhos = config.getFleetSizes();
        String[] nomes = config.getFleetNames();
        char colStart = config.getColStart();
        char colEnd = config.getColEnd();
        int n = config.getBoardSize();

        if (erro != null) System.out.println("  ✗ " + erro);

        System.out.println("\nNavio: " + nomes[sid] + " (tamanho " + tamanhos[sid] + ")");
        System.out.println("  Digite 'auto' para posicionamento automático.");
        System.out.print("  Coordenada inicial (" + colStart + "1-" + colEnd + n + "): ");
        String coordStr = sc.nextLine().trim();
        if (coordStr.equalsIgnoreCase("auto")) return null;

        Coordenada coord = Coordenada.parse(coordStr, colStart, colEnd, n);
        if (coord == null) {
            return pedirPosicionamento(sid, "Coordenada inválida.");
        }

        System.out.print("  Direção (H=horizontal, V=vertical): ");
        String dir = sc.nextLine().trim().toUpperCase(Locale.ROOT);
        if (!dir.equals("H") && !dir.equals("V")) {
            return pedirPosicionamento(sid, "Direção inválida. Use H ou V.");
        }

        return new FrotaService.PlacementInput(coord, dir.equals("H"));
    }

    public void mostrarTabuleiro(String titulo, char[][] grade, boolean ocultarNavios, int n) {
        char colStart = config.getColStart();
        System.out.println(titulo);
        System.out.print("    ");
        for (int c = 0; c < n; c++) System.out.print((char)(colStart + c) + " ");
        System.out.println();

        for (int r = 0; r < n; r++) {
            System.out.printf("%2d  ", r + 1);
            for (int c = 0; c < n; c++) {
                char cel = grade[r][c];
                if (ocultarNavios && cel == Tabuleiro.NAVIO) cel = Tabuleiro.VAZIO;
                System.out.print(cel + " ");
            }
            System.out.println();
        }
    }

    // ====== Tabuleiros lado a lado ======

    public void mostrarDoisTabuleiros(char[][] meuGrade, char[][] tirosGrade, int n) {
        char colStart = config.getColStart();
        boolean showOwn = config.isShowOwnShips();

        String cabTitulo = String.format("%-28s| %s", "SEU TABULEIRO", "SEUS TIROS NO INIMIGO");
        System.out.println(cabTitulo);

        // Cabeçalho de colunas
        StringBuilder cabCols = new StringBuilder("    ");
        for (int c = 0; c < n; c++) cabCols.append((char)(colStart + c)).append(' ');
        cabCols.append("   |     ");
        for (int c = 0; c < n; c++) cabCols.append((char)(colStart + c)).append(' ');
        System.out.println(cabCols);

        for (int r = 0; r < n; r++) {
            StringBuilder linha = new StringBuilder(String.format("%2d  ", r + 1));
            for (int c = 0; c < n; c++) {
                char cel = meuGrade[r][c];
                if (!showOwn && cel == Tabuleiro.NAVIO) cel = Tabuleiro.VAZIO;
                linha.append(cel).append(' ');
            }
            linha.append("  |  ");
            linha.append(String.format("%2d  ", r + 1));
            for (int c = 0; c < n; c++) {
                char cel = tirosGrade[r][c];
                if (cel == Tabuleiro.NAVIO) cel = Tabuleiro.VAZIO;
                linha.append(cel).append(' ');
            }
            System.out.println(linha);
        }

        if (config.isShowLegend()) {
            System.out.println("Legenda: S=navio  X=acerto  o=água  .=vazio/desconhecido");
        }
    }

    public void mostrarStatus(long meuNaviosVivos, long cpuNaviosVivos) {
        System.out.printf("Navios vivos — Você: %d | CPU: %d%n", meuNaviosVivos, cpuNaviosVivos);
    }

    // ====== Turno do jogador ======

    /** Retorna a opção escolhida no menu do turno. */
    public String lerMenuTurno() {
        System.out.println("\n--- Seu turno ---");
        System.out.println("  [1] Atirar   [2] Ver log   [3] Meu tabuleiro   [?] Ajuda");
        System.out.print("> ");
        return sc.nextLine().trim();
    }

    /** Lê e valida uma coordenada para tiro. Retorna null se digitou "?" para ajuda. */
    public Coordenada lerCoordenadaTiro(Tabuleiro alvo) {
        char colStart = config.getColStart();
        char colEnd = config.getColEnd();
        int n = config.getBoardSize();

        while (true) {
            System.out.print("  Coordenada para atirar (ex " + colStart + "5): ");
            String s = sc.nextLine().trim();
            if (s.equals("?")) return null;

            Coordenada c = Coordenada.parse(s, colStart, colEnd, n);
            if (c == null) {
                System.out.println("  ✗ Coordenada inválida.");
                continue;
            }
            if (alvo.foiAtirado(c.getRow(), c.getCol())) {
                System.out.println("  ✗ Você já atirou nessa posição.");
                continue;
            }
            return c;
        }
    }

    // ====== Exibição de resultados ======

    public void exibirResultados(List<ResultadoTiro> resultados, char colStart) {
        for (ResultadoTiro r : resultados) {
            String coord = r.getCoordenada().toDisplay(colStart);
            String msg = switch (r.getTipo()) {
                case AGUA       -> "💧 ÁGUA em " + coord;
                case ACERTO     -> "💥 ACERTO em " + coord + "!";
                case AFUNDOU    -> "⚓ AFUNDOU o " + r.getNavioAfundado().getNome() + " em " + coord + "!";
                case JA_ATIRADO -> "⚠  Já atirou em " + coord + ". Escolha outra.";
            };
            System.out.println("  " + msg);
        }
    }

    public void exibirResultadosCpu(List<ResultadoTiro> resultados, char colStart) {
        System.out.println("\n--- Turno da CPU ---");
        for (ResultadoTiro r : resultados) {
            String coord = r.getCoordenada().toDisplay(colStart);
            String msg = switch (r.getTipo()) {
                case AGUA    -> "CPU errou em " + coord + ".";
                case ACERTO  -> "CPU acertou em " + coord + "!";
                case AFUNDOU -> "CPU afundou seu " + r.getNavioAfundado().getNome() + " em " + coord + "!";
                default      -> "";
            };
            System.out.println("  " + msg);
        }
    }

    public void exibirFimDeJogo(String vencedor, String meuNome) {
        System.out.println("\n" + "=".repeat(40));
        if (vencedor != null && vencedor.equals(meuNome)) {
            System.out.println("🏆 VITÓRIA! Você afundou toda a frota inimiga!");
        } else {
            System.out.println("💀 DERROTA! Sua frota foi afundada.");
        }
        System.out.println("=".repeat(40));
    }

    // ====== Log ======

    public void exibirLog(List<batalhanaval.domain.Jogada> jogadas, int ultimos) {
        System.out.println("\n--- Últimos " + ultimos + " eventos ---");
        int start = Math.max(0, jogadas.size() - ultimos);
        for (int i = start; i < jogadas.size(); i++) {
            var j = jogadas.get(i);
            System.out.printf("  T%d [%s] %s → %s%n",
                    j.turno(), j.jogador(), j.coordenada(), j.resultado());
        }
    }

    public void exibirLogCompleto(List<batalhanaval.domain.Jogada> jogadas) {
        System.out.println("\n--- Log completo ---");
        for (int i = 0; i < jogadas.size(); i++) {
            var j = jogadas.get(i);
            System.out.printf("  %3d) T%d [%s] %s → %s%n",
                    i + 1, j.turno(), j.jogador(), j.coordenada(), j.resultado());
        }
    }

    public void perguntarMostrarLogCompleto(List<batalhanaval.domain.Jogada> jogadas) {
        System.out.print("\nMostrar log completo? (s/N): ");
        String r = sc.nextLine().trim().toLowerCase(Locale.ROOT);
        if (r.equals("s") || r.equals("sim")) exibirLogCompleto(jogadas);
    }

    // ====== Ajuda ======

    public void exibirAjuda() {
        System.out.println("""

            === AJUDA ===
            Coordenadas: letra (coluna) + número (linha). Ex: A1, J10.
            No seu tabuleiro: S=navio, X=acertado, .=vazio.
            Nos seus tiros:   X=acerto, o=água, .=não atirado.
            =============
            """);
    }

    // ====== Histórico ======

    public void exibirHistorico(List<GameRepository.PartidaResumo> partidas) {
        if (partidas.isEmpty()) {
            System.out.println("Nenhuma partida salva.");
            return;
        }
        System.out.println("\n--- Histórico de partidas ---");
        System.out.printf("  %-5s %-22s %-22s %-15s %s%n",
                "ID", "Início", "Fim", "Vencedor", "Seed");
        for (var p : partidas) {
            System.out.printf("  %-5d %-22s %-22s %-15s %s%n",
                    p.id(),
                    p.inicio() != null ? p.inicio() : "-",
                    p.fim() != null ? p.fim() : "-",
                    p.vencedor() != null ? p.vencedor() : "-",
                    p.seed() != null ? p.seed() : "-");
        }
    }

    // ====== Replay ======

    public long lerIdPartida() {
        System.out.print("ID da partida para replay: ");
        try { return Long.parseLong(sc.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    public void exibirReplay(GameRepository.PartidaResumo partida,
                             List<batalhanaval.domain.Jogada> jogadas, int delayMs) {
        System.out.println("\n=== REPLAY Partida #" + partida.id() + " ===");
        System.out.println("Início: " + partida.inicio());
        System.out.println("Vencedor: " + (partida.vencedor() != null ? partida.vencedor() : "-"));
        System.out.println("Pressione ENTER para avançar cada jogada (ou digite 'auto' para avançar sozinho).");

        boolean auto = false;
        for (int i = 0; i < jogadas.size(); i++) {
            var j = jogadas.get(i);
            System.out.printf("[%d/%d] T%d [%s] %s → %s",
                    i + 1, jogadas.size(), j.turno(), j.jogador(), j.coordenada(), j.resultado());

            if (auto) {
                System.out.println();
                if (delayMs > 0) {
                    try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
                }
            } else {
                System.out.print("  [ENTER / 'auto'] ");
                String inp = sc.nextLine().trim().toLowerCase(Locale.ROOT);
                if (inp.equals("auto")) auto = true;
            }
        }
        System.out.println("\n=== Fim do replay ===");
    }

    // ====== Genéricos ======

    public void println(String msg) { System.out.println(msg); }
    public void print(String msg)   { System.out.print(msg); }
    public String lerLinha()        { return sc.nextLine().trim(); }
}
