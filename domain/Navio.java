package batalhanaval.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa um navio com suas células ocupadas e contagem de acertos.
 */
public class Navio {

    private final int id;
    private final String nome;
    private final int tamanho;
    private final List<Coordenada> celulas;
    private int acertos;

    public Navio(int id, String nome, int tamanho) {
        this.id = id;
        this.nome = nome;
        this.tamanho = tamanho;
        this.celulas = new ArrayList<>(tamanho);
        this.acertos = 0;
    }

    public void adicionarCelula(Coordenada c) {
        celulas.add(c);
    }

    public boolean ocupaCelula(Coordenada c) {
        return celulas.contains(c);
    }

    /** Registra um acerto. Retorna true se o navio foi afundado neste tiro. */
    public boolean registrarAcerto() {
        acertos++;
        return estaAfundado();
    }

    public boolean estaAfundado() {
        return acertos >= tamanho;
    }

    public int getId()          { return id; }
    public String getNome()     { return nome; }
    public int getTamanho()     { return tamanho; }
    public int getAcertos()     { return acertos; }

    public List<Coordenada> getCelulas() {
        return Collections.unmodifiableList(celulas);
    }

    @Override
    public String toString() {
        return nome + "(tam=" + tamanho + ", acertos=" + acertos + ")";
    }
}
