package batalhanaval.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * Representa uma coordenada (coluna, linha) do tabuleiro.
 * col: índice 0-based (0 = 'A').
 * row: índice 0-based (0 = linha 1).
 */
public final class Coordenada {

    private final int col;
    private final int row;

    public Coordenada(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int getCol() { return col; }
    public int getRow() { return row; }

    /**
     * Tenta fazer parse de uma string como "A1", "J10", etc.
     * colStart e colEnd delimitam o intervalo válido de colunas.
     * boardSize delimita o intervalo válido de linhas (1..boardSize).
     * Retorna null se inválida.
     */
    public static Coordenada parse(String s, char colStart, char colEnd, int boardSize) {
        if (s == null) return null;
        String t = s.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (t.length() < 2 || t.length() > 3) return null;

        char colChar = t.charAt(0);
        if (colChar < colStart || colChar > colEnd) return null;
        int col = colChar - colStart;

        String numStr = t.substring(1);
        int row;
        try { row = Integer.parseInt(numStr); }
        catch (NumberFormatException e) { return null; }
        if (row < 1 || row > boardSize) return null;

        return new Coordenada(col, row - 1);
    }

    /** Representação amigável ex: "A1" */
    public String toDisplay(char colStart) {
        return "" + (char)(colStart + col) + (row + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordenada c)) return false;
        return col == c.col && row == c.row;
    }

    @Override
    public int hashCode() { return Objects.hash(col, row); }

    @Override
    public String toString() { return "Coordenada{col=" + col + ", row=" + row + "}"; }
}
