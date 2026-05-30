package batalhanaval;

import batalhanaval.config.GameConfig;
import batalhanaval.domain.*;
import batalhanaval.persistence.GameRepository;
import batalhanaval.service.FrotaService;
import batalhanaval.ui.TerminalUI;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // Carrega configuração
        String configPath = args.length > 0 ? args[0] : "game.properties";
        GameConfig config = new GameConfig(configPath);

        Scanner sc = new Scanner(System.in);
        TerminalUI ui = new TerminalUI(config, sc);

        ui.mostrarBemVindo();

        // Repositório
        GameRepository repo = null;
        if (config.isDbEnabled()) {
            repo = new GameRepository(config.getSqliteFile());
            try {
                repo.open();
                if (config.isAutoMigrate()) repo.criarTabelas();
            } catch (SQLException e) {
                ui.println("Aviso: banco de dados indisponível. (" + e.getMessage() + ")");
                repo = null;
            }
        }

        // Modo de execução via config
        String modeFromConfig = config.getGameMode();

        boolean rodando = true;
        while (rodando) {
            String opcao;
            if (modeFromConfig.equals("LIST")) {
                opcao = "2"; modeFromConfig = "MENU";
            } else if (modeFromConfig.equals("REPLAY")) {
                opcao = "3"; modeFromConfig = "MENU";
            } else {
                opcao = ui.lerMenuInicial();
            }

            switch (opcao) {
                case "1" -> jogarPartida(config, ui, repo);
                case "2" -> listarHistorico(ui, repo);
                case "3" -> replay(config, ui, repo);
                case "X", "S", "SAIR" -> rodando = false;
                default -> ui.println("Opção inválida.");
            }
        }

        ui.println("\nAté logo!");
        if (repo != null) repo.close();
        sc.close();
    }

    // ====== Fluxo principal de uma partida ======

    private static void jogarPartida(GameConfig config, TerminalUI ui, GameRepository repo) {
        int n = config.getBoardSize();
        char colStart = config.getColStart();

        // Seed
        Long seed = config.getGameSeed();
        Random rng = (seed != null) ? new Random(seed) : new Random();
        if (seed == null) seed = rng.nextLong(); // captura seed usada

        ui.println("\n--- Nova Partida (seed: " + seed + ") ---");

        // Tabuleiros
        Tabuleiro tabJogador = new Tabuleiro(n);
        Tabuleiro tabCpu     = new Tabuleiro(n);

        FrotaService frotaService = new FrotaService(config);

        // ---- Posicionamento do jogador ----
        ui.println("\nPosicionando sua frota...");
        boolean manual = ui.perguntarPosicionamentoManual();

        final Tabuleiro tabJogadorRef = tabJogador;
        if (manual) {
            boolean ok = frotaService.posicionarManual(tabJogador, (sid, erro) -> {
                ui.mostrarTabuleiro("Seu tabuleiro:", tabJogadorRef.getGrade(), false, n);
                return ui.pedirPosicionamento(sid, erro);
            });
            if (!ok) {
                // fallback automático
                ui.println("Posicionamento automático ativado.");
                tabJogador = new Tabuleiro(n);
                frotaService.posicionarAutomatico(tabJogador, rng);
            }
        } else {
            frotaService.posicionarAutomatico(tabJogador, rng);
            ui.println("Frota posicionada automaticamente.");
        }

        // ---- Posicionamento CPU ----
        frotaService.posicionarAutomatico(tabCpu, rng);

        // ---- Cria jogadores ----
        final Tabuleiro tabJogadorFinal = tabJogador;
        final Tabuleiro tabCpuFinal     = tabCpu;

        HumanPlayer jogador = new HumanPlayer("Jogador", tabJogadorFinal, tabCpuFinal,
                alvo -> ui.lerCoordenadaTiro(alvo));

        CpuPlayer cpu = new CpuPlayer("CPU", tabCpuFinal, tabJogadorFinal,
                config.getCpuStrategy(), config.isCpuParityPreference(), rng);

        // ---- Jogo ----
        Jogo jogo = new Jogo(jogador, cpu, colStart,
                config.isHitGrantsExtraShot(), config.getMaxExtraShots(), seed);

        while (jogo.isEmAndamento()) {
            // Exibe estado
            ui.mostrarDoisTabuleiros(tabJogadorFinal.getGrade(), jogador.getTabuleirAlvo().getGrade(), n);
            ui.mostrarStatus(tabJogadorFinal.naviosVivos(), tabCpuFinal.naviosVivos());

            // Menu do turno
            String opt = ui.lerMenuTurno();
            switch (opt) {
                case "2" -> { ui.exibirLog(jogo.getJogadas(), 10); continue; }
                case "3" -> { ui.mostrarTabuleiro("Seu tabuleiro:",
                        tabJogadorFinal.getGrade(), false, n); continue; }
                case "?" -> { ui.exibirAjuda(); continue; }
            }

            // Tiro do jogador
            List<ResultadoTiro> resJogador = jogo.turnoJogador();
            ui.exibirResultados(resJogador, colStart);

            if (!jogo.isEmAndamento()) break;

            // Turno CPU
            List<ResultadoTiro> resCpu = jogo.turnoCpu();
            ui.exibirResultadosCpu(resCpu, colStart);
        }

        // Exibe fim de jogo
        ui.mostrarDoisTabuleiros(tabJogadorFinal.getGrade(), jogador.getTabuleirAlvo().getGrade(), n);
        ui.exibirFimDeJogo(jogo.getVencedor(), jogador.getNome());
        ui.perguntarMostrarLogCompleto(jogo.getJogadas());

        // Persistência
        if (repo != null) {
            try {
                long id = repo.salvarPartida(jogo, config.isSaveInitialFleet());
                ui.println("Partida salva com ID #" + id + ".");
            } catch (SQLException e) {
                ui.println("Erro ao salvar partida: " + e.getMessage());
            }
        }
    }

    // ====== Histórico ======

    private static void listarHistorico(TerminalUI ui, GameRepository repo) {
        if (repo == null) { ui.println("Banco desabilitado."); return; }
        try {
            ui.exibirHistorico(repo.listarPartidas());
        } catch (SQLException e) {
            ui.println("Erro ao listar: " + e.getMessage());
        }
    }

    // ====== Replay ======

    private static void replay(GameConfig config, TerminalUI ui, GameRepository repo) {
        if (repo == null) { ui.println("Banco desabilitado."); return; }
        try {
            listarHistorico(ui, repo);
            long id = ui.lerIdPartida();
            if (id < 0) { ui.println("ID inválido."); return; }

            GameRepository.PartidaResumo partida = repo.buscarPartida(id);
            if (partida == null) { ui.println("Partida não encontrada."); return; }

            List<Jogada> jogadas = repo.buscarJogadas(id);
            ui.exibirReplay(partida, jogadas, config.getReplayDelayMs());
        } catch (SQLException e) {
            ui.println("Erro no replay: " + e.getMessage());
        }
    }
}
