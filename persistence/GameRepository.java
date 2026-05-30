package batalhanaval.persistence;

import batalhanaval.domain.Jogada;
import batalhanaval.domain.Jogo;
import batalhanaval.domain.Navio;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositório SQLite. Todo SQL fica aqui. Domínio e UI não tocam em SQL.
 */
public class GameRepository {

    private final String dbFile;
    private Connection conn;

    public GameRepository(String dbFile) {
        this.dbFile = dbFile;
    }

    // ---- Ciclo de vida ----

    public void open() throws SQLException {
        // Garante que o diretório existe
        File f = new File(dbFile);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();

        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        conn.setAutoCommit(false);
    }

    public void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (SQLException ignored) {}
    }

    // ---- Auto-migrate ----

    public void criarTabelas() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS partidas (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    inicio    TEXT NOT NULL,
                    fim       TEXT,
                    vencedor  TEXT,
                    seed      TEXT
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS jogadores (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    partida_id INTEGER NOT NULL,
                    nome       TEXT NOT NULL,
                    tipo       TEXT NOT NULL,
                    FOREIGN KEY (partida_id) REFERENCES partidas(id)
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS jogadas (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    partida_id INTEGER NOT NULL,
                    turno      INTEGER NOT NULL,
                    jogador    TEXT NOT NULL,
                    coordenada TEXT NOT NULL,
                    resultado  TEXT NOT NULL,
                    FOREIGN KEY (partida_id) REFERENCES partidas(id)
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS frota_inicial (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    partida_id INTEGER NOT NULL,
                    jogador    TEXT NOT NULL,
                    navio      TEXT NOT NULL,
                    tamanho    INTEGER NOT NULL,
                    celulas    TEXT NOT NULL,
                    FOREIGN KEY (partida_id) REFERENCES partidas(id)
                )
            """);
            conn.commit();
        }
    }

    // ---- Salvar partida completa ----

    public long salvarPartida(Jogo jogo, boolean saveInitialFleet) throws SQLException {
        long partidaId;

        // Insere partida
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO partidas (inicio, fim, vencedor, seed) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, jogo.getInicio().toString());
            ps.setString(2, jogo.getFim() != null ? jogo.getFim().toString() : null);
            ps.setString(3, jogo.getVencedor());
            ps.setString(4, jogo.getSeed() != null ? jogo.getSeed().toString() : null);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                partidaId = rs.getLong(1);
            }
        }

        // Insere jogadores
        inserirJogador(partidaId, jogo.getJogador().getNome(), jogo.getJogador().getTipo());
        inserirJogador(partidaId, jogo.getCpu().getNome(), jogo.getCpu().getTipo());

        // Insere jogadas
        for (Jogada j : jogo.getJogadas()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO jogadas (partida_id, turno, jogador, coordenada, resultado) VALUES (?,?,?,?,?)")) {
                ps.setLong(1, partidaId);
                ps.setInt(2, j.turno());
                ps.setString(3, j.jogador());
                ps.setString(4, j.coordenada());
                ps.setString(5, j.resultado());
                ps.executeUpdate();
            }
        }

        // Salva frota inicial se configurado
        if (saveInitialFleet) {
            salvarFrotaInicial(partidaId, jogo.getJogador().getNome(),
                    jogo.getJogador().getTabuleiro().getNavios());
            salvarFrotaInicial(partidaId, jogo.getCpu().getNome(),
                    jogo.getCpu().getTabuleiro().getNavios());
        }

        conn.commit();
        return partidaId;
    }

    private void inserirJogador(long partidaId, String nome, String tipo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO jogadores (partida_id, nome, tipo) VALUES (?,?,?)")) {
            ps.setLong(1, partidaId);
            ps.setString(2, nome);
            ps.setString(3, tipo);
            ps.executeUpdate();
        }
    }

    private void salvarFrotaInicial(long partidaId, String jogador,
                                    List<Navio> navios) throws SQLException {
        for (Navio n : navios) {
            StringBuilder sb = new StringBuilder();
            n.getCelulas().forEach(c -> {
                if (!sb.isEmpty()) sb.append(';');
                sb.append(c.getCol()).append(',').append(c.getRow());
            });
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO frota_inicial (partida_id, jogador, navio, tamanho, celulas) VALUES (?,?,?,?,?)")) {
                ps.setLong(1, partidaId);
                ps.setString(2, jogador);
                ps.setString(3, n.getNome());
                ps.setInt(4, n.getTamanho());
                ps.setString(5, sb.toString());
                ps.executeUpdate();
            }
        }
    }

    // ---- Listar histórico ----

    public List<PartidaResumo> listarPartidas() throws SQLException {
        List<PartidaResumo> lista = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, inicio, fim, vencedor, seed FROM partidas ORDER BY id DESC")) {
            while (rs.next()) {
                lista.add(new PartidaResumo(
                        rs.getLong("id"),
                        rs.getString("inicio"),
                        rs.getString("fim"),
                        rs.getString("vencedor"),
                        rs.getString("seed")
                ));
            }
        }
        return lista;
    }

    // ---- Replay ----

    public List<Jogada> buscarJogadas(long partidaId) throws SQLException {
        List<Jogada> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT turno, jogador, coordenada, resultado FROM jogadas WHERE partida_id=? ORDER BY id")) {
            ps.setLong(1, partidaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Jogada(
                            rs.getInt("turno"),
                            rs.getString("jogador"),
                            rs.getString("coordenada"),
                            rs.getString("resultado")
                    ));
                }
            }
        }
        return lista;
    }

    public PartidaResumo buscarPartida(long partidaId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, inicio, fim, vencedor, seed FROM partidas WHERE id=?")) {
            ps.setLong(1, partidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PartidaResumo(
                            rs.getLong("id"),
                            rs.getString("inicio"),
                            rs.getString("fim"),
                            rs.getString("vencedor"),
                            rs.getString("seed")
                    );
                }
            }
        }
        return null;
    }

    // ---- DTO ----

    public record PartidaResumo(long id, String inicio, String fim,
                                String vencedor, String seed) {}
}
