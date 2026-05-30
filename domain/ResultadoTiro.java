package batalhanaval.domain;

/**
 * Resultado de um tiro aplicado ao tabuleiro.
 */
public class ResultadoTiro {

    public enum Tipo { AGUA, ACERTO, AFUNDOU, JA_ATIRADO }

    private final Tipo tipo;
    private final Navio navioAfundado; // não-null apenas quando AFUNDOU
    private final Coordenada coordenada;

    public ResultadoTiro(Tipo tipo, Coordenada coordenada, Navio navioAfundado) {
        this.tipo = tipo;
        this.coordenada = coordenada;
        this.navioAfundado = navioAfundado;
    }

    public static ResultadoTiro agua(Coordenada c) {
        return new ResultadoTiro(Tipo.AGUA, c, null);
    }

    public static ResultadoTiro acerto(Coordenada c) {
        return new ResultadoTiro(Tipo.ACERTO, c, null);
    }

    public static ResultadoTiro afundou(Coordenada c, Navio n) {
        return new ResultadoTiro(Tipo.AFUNDOU, c, n);
    }

    public static ResultadoTiro jaAtirado(Coordenada c) {
        return new ResultadoTiro(Tipo.JA_ATIRADO, c, null);
    }

    public Tipo getTipo()               { return tipo; }
    public Navio getNavioAfundado()     { return navioAfundado; }
    public Coordenada getCoordenada()   { return coordenada; }

    public boolean isHit() {
        return tipo == Tipo.ACERTO || tipo == Tipo.AFUNDOU;
    }
}
