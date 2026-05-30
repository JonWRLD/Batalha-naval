package batalhanaval.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controla o fluxo de turnos e o estado do jogo.
 * Sem Scanner, sem System.out. A UI chama os métodos e exibe os resultados.
 */
public class Jogo {

    public enum Estado { EM_ANDAMENTO, JOGADOR_VENCEU, CPU_VENCEU }

    private final Jogador jogador;
    private final Jogador cpu;
    private final char colStart;
    private final boolean hitGrantsExtraShot;
    private final int maxExtraShots;

    private Estado estado;
    private int turno;
    private final List<Jogada> jogadas;
    private final LocalDateTime inicio;
    private LocalDateTime fim;
    private final Long seed;

    public Jogo(Jogador jogador, Jogador cpu, char colStart,
                boolean hitGrantsExtraShot, int maxExtraShots, Long seed) {
        this.jogador = jogador;
        this.cpu = cpu;
        this.colStart = colStart;
        this.hitGrantsExtraShot = hitGrantsExtraShot;
        this.maxExtraShots = maxExtraShots;
        this.estado = Estado.EM_ANDAMENTO;
        this.turno = 0;
        this.jogadas = new ArrayList<>();
        this.inicio = LocalDateTime.now();
        this.seed = seed;
    }

    /** Executa um turno completo do jogador humano. Retorna os resultados. */
    public List<ResultadoTiro> turnoJogador() {
        turno++;
        List<ResultadoTiro> resultados = new ArrayList<>();
        int extras = 0;

        do {
            Coordenada alvo = jogador.escolherAlvo();
            ResultadoTiro resultado = cpu.getTabuleiro().atirar(alvo);

            if (resultado.getTipo() == ResultadoTiro.Tipo.JA_ATIRADO) {
                resultados.add(resultado);
                continue; // a UI deve pedir nova coordenada; não conta como extra
            }

            registrarJogada(turno, jogador.getNome(), alvo, resultado);
            resultados.add(resultado);

            if (cpu.perdeu()) {
                estado = Estado.JOGADOR_VENCEU;
                fim = LocalDateTime.now();
                break;
            }

            if (hitGrantsExtraShot && resultado.isHit() && extras < maxExtraShots) {
                extras++;
            } else {
                break;
            }
        } while (true);

        return resultados;
    }

    /** Executa o turno da CPU. Retorna os resultados. */
    public List<ResultadoTiro> turnoCpu() {
        List<ResultadoTiro> resultados = new ArrayList<>();
        int extras = 0;

        do {
            Coordenada alvo = cpu.escolherAlvo();
            if (alvo == null) break;

            ResultadoTiro resultado = jogador.getTabuleiro().atirar(alvo);
            cpu.notificarResultado(resultado);

            registrarJogada(turno, cpu.getNome(), alvo, resultado);
            resultados.add(resultado);

            if (jogador.perdeu()) {
                estado = Estado.CPU_VENCEU;
                fim = LocalDateTime.now();
                break;
            }

            if (hitGrantsExtraShot && resultado.isHit() && extras < maxExtraShots) {
                extras++;
            } else {
                break;
            }
        } while (true);

        return resultados;
    }

    private void registrarJogada(int t, String nome, Coordenada c, ResultadoTiro r) {
        String coord = c.toDisplay(colStart);
        String res = switch (r.getTipo()) {
            case AGUA    -> "AGUA";
            case ACERTO  -> "ACERTO";
            case AFUNDOU -> "AFUNDOU:" + (r.getNavioAfundado() != null ? r.getNavioAfundado().getNome() : "");
            default      -> "?";
        };
        jogadas.add(new Jogada(t, nome, coord, res));
    }

    public Estado getEstado()         { return estado; }
    public boolean isEmAndamento()    { return estado == Estado.EM_ANDAMENTO; }
    public int getTurno()             { return turno; }
    public List<Jogada> getJogadas()  { return Collections.unmodifiableList(jogadas); }
    public Jogador getJogador()       { return jogador; }
    public Jogador getCpu()           { return cpu; }
    public LocalDateTime getInicio()  { return inicio; }
    public LocalDateTime getFim()     { return fim; }
    public Long getSeed()             { return seed; }

    public String getVencedor() {
        return switch (estado) {
            case JOGADOR_VENCEU -> jogador.getNome();
            case CPU_VENCEU     -> cpu.getNome();
            default             -> null;
        };
    }
}
