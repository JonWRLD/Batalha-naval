package batalhanaval.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Lê e expõe as configurações do game.properties.
 * O programa nunca deve usar valores fixos para o que está aqui.
 */
public class GameConfig {

    private final Properties props;

    public GameConfig(String path) {
        props = new Properties();
        // tenta carregar do caminho fornecido; se não existir, usa o classpath
        try (InputStream is = new FileInputStream(path)) {
            props.load(is);
        } catch (IOException e) {
            try (InputStream is = GameConfig.class.getClassLoader()
                    .getResourceAsStream("game.properties")) {
                if (is != null) props.load(is);
            } catch (IOException ex) {
                // sem arquivo: usa defaults abaixo
            }
        }
    }

    // ---- Tabuleiro ----
    public int getBoardSize() {
        return Integer.parseInt(props.getProperty("board.size", "10"));
    }

    public char getColStart() {
        String s = props.getProperty("board.columns.start", "A");
        return s.isEmpty() ? 'A' : s.charAt(0);
    }

    public char getColEnd() {
        String s = props.getProperty("board.columns.end", "J");
        return s.isEmpty() ? 'J' : s.charAt(0);
    }

    // ---- Frota ----
    public int[] getFleetSizes() {
        String raw = props.getProperty("fleet.sizes", "5,4,3,3,2");
        return Arrays.stream(raw.split(","))
                .mapToInt(t -> Integer.parseInt(t.trim()))
                .toArray();
    }

    public String[] getFleetNames() {
        String raw = props.getProperty("fleet.names",
                "Porta-avioes,Encouracado,Cruzador,Submarino,Destroyer");
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .toArray(String[]::new);
    }

    /** NONE | ORTHO | ORTHO_DIAG */
    public String getAdjacencyRule() {
        return props.getProperty("fleet.adjacency_rule", "ORTHO_DIAG").trim().toUpperCase();
    }

    // ---- Regras de turno ----
    public boolean isHitGrantsExtraShot() {
        return Boolean.parseBoolean(props.getProperty("rules.hit_grants_extra_shot", "false"));
    }

    public int getMaxExtraShots() {
        return Integer.parseInt(props.getProperty("rules.max_extra_shots", "3"));
    }

    // ---- CPU ----
    /** RANDOM | HUNT | PARITY */
    public String getCpuStrategy() {
        return props.getProperty("cpu.strategy", "HUNT").trim().toUpperCase();
    }

    public boolean isCpuParityPreference() {
        return Boolean.parseBoolean(props.getProperty("cpu.use_parity_preference", "true"));
    }

    // ---- UI ----
    public boolean isShowOwnShips() {
        return Boolean.parseBoolean(props.getProperty("ui.show_own_ships", "true"));
    }

    public boolean isShowLegend() {
        return Boolean.parseBoolean(props.getProperty("ui.show_legend", "true"));
    }

    public int getReplayDelayMs() {
        return Integer.parseInt(props.getProperty("ui.replay_delay_ms", "250"));
    }

    // ---- Banco ----
    public boolean isDbEnabled() {
        return Boolean.parseBoolean(props.getProperty("db.enabled", "true"));
    }

    public String getDbType() {
        return props.getProperty("db.type", "sqlite").trim().toLowerCase();
    }

    public String getSqliteFile() {
        return props.getProperty("db.sqlite.file", "data/batalha_naval.db");
    }

    public boolean isAutoMigrate() {
        return Boolean.parseBoolean(props.getProperty("db.auto_migrate", "true"));
    }

    public boolean isSaveInitialFleet() {
        return Boolean.parseBoolean(props.getProperty("db.save_initial_fleet", "true"));
    }

    // ---- Execução ----
    public Long getGameSeed() {
        String s = props.getProperty("game.seed", "").trim();
        if (s.isEmpty()) return null;
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return (long) s.hashCode(); }
    }

    /** PLAY | LIST | REPLAY */
    public String getGameMode() {
        return props.getProperty("game.mode", "PLAY").trim().toUpperCase();
    }

    // ---- Identificação do grupo ----
    public String getGroupId()   { return props.getProperty("group.id", "G00"); }
    public String getGroupName() { return props.getProperty("group.name", "Grupo"); }
    public String getVersion()   { return props.getProperty("project.version", "1.0.0"); }
}
