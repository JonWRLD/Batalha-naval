package batalhanaval;

import batalhanaval.domain.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do motor do jogo. Sem Scanner, sem prints, sem terminal.
 */
class BatalhaNavalTest {

    // ===== Coordenada =====

    @Test
    void coordenadaValida() {
        Coordenada c = Coordenada.parse("A1", 'A', 'J', 10);
        assertNotNull(c);
        assertEquals(0, c.getCol());
        assertEquals(0, c.getRow());
    }

    @Test
    void coordenadaValida_J10() {
        Coordenada c = Coordenada.parse("J10", 'A', 'J', 10);
        assertNotNull(c);
        assertEquals(9, c.getCol());
        assertEquals(9, c.getRow());
    }

    @Test
    void coordenadaInvalida_foraDaColuna() {
        assertNull(Coordenada.parse("K1", 'A', 'J', 10));
    }

    @Test
    void coordenadaInvalida_foraLinha() {
        assertNull(Coordenada.parse("A11", 'A', 'J', 10));
    }

    @Test
    void coordenadaInvalida_nula() {
        assertNull(Coordenada.parse(null, 'A', 'J', 10));
    }

    @Test
    void coordenadaInvalida_string_vazia() {
        assertNull(Coordenada.parse("", 'A', 'J', 10));
    }

    @Test
    void coordenadaToDisplay() {
        Coordenada c = new Coordenada(2, 4);
        assertEquals("C5", c.toDisplay('A'));
    }

    // ===== Navio =====

    @Test
    void navioNaoAfundadoInicial() {
        Navio n = new Navio(0, "Destroyer", 2);
        assertFalse(n.estaAfundado());
    }

    @Test
    void navioAfundaAposAcertos() {
        Navio n = new Navio(0, "Destroyer", 2);
        assertFalse(n.registrarAcerto()); // 1 acerto, ainda não afundou
        assertTrue(n.registrarAcerto());  // 2 acertos, afundou
    }

    // ===== Tabuleiro - posicionamento =====

    @Test
    void posicionarNavioHorizontal_valido() {
        Tabuleiro t = new Tabuleiro(10);
        Navio n = new Navio(0, "Destroyer", 2);
        boolean ok = t.posicionarNavio(n, new Coordenada(0, 0), true, "NONE");
        assertTrue(ok);
        assertEquals(Tabuleiro.NAVIO, t.getCelula(0, 0));
        assertEquals(Tabuleiro.NAVIO, t.getCelula(0, 1));
    }

    @Test
    void posicionarNavioVertical_valido() {
        Tabuleiro t = new Tabuleiro(10);
        Navio n = new Navio(0, "Destroyer", 3);
        boolean ok = t.posicionarNavio(n, new Coordenada(0, 0), false, "NONE");
        assertTrue(ok);
        assertEquals(Tabuleiro.NAVIO, t.getCelula(0, 0));
        assertEquals(Tabuleiro.NAVIO, t.getCelula(1, 0));
        assertEquals(Tabuleiro.NAVIO, t.getCelula(2, 0));
    }

    @Test
    void posicionarNavio_foraDosLimites() {
        Tabuleiro t = new Tabuleiro(10);
        Navio n = new Navio(0, "Destroyer", 5);
        // horizontal a partir de col=8 com tamanho 5 não cabe
        boolean ok = t.posicionarNavio(n, new Coordenada(8, 0), true, "NONE");
        assertFalse(ok);
    }

    @Test
    void posicionarNavio_colisao() {
        Tabuleiro t = new Tabuleiro(10);
        Navio n1 = new Navio(0, "N1", 3);
        Navio n2 = new Navio(1, "N2", 3);
        t.posicionarNavio(n1, new Coordenada(0, 0), true, "NONE");
        boolean ok = t.posicionarNavio(n2, new Coordenada(1, 0), true, "NONE");
        assertFalse(ok);
    }

    @Test
    void posicionarNavio_adjacenciaORTHO() {
        Tabuleiro t = new Tabuleiro(10);
        Navio n1 = new Navio(0, "N1", 2);
        Navio n2 = new Navio(1, "N2", 2);
        t.posicionarNavio(n1, new Coordenada(0, 0), true, "ORTHO");
        // n2 na linha adjacente deve ser rejeitado com ORTHO
        boolean ok = t.posicionarNavio(n2, new Coordenada(0, 1), true, "ORTHO");
        assertFalse(ok);
    }

    // ===== Tabuleiro - tiros =====

    @Test
    void tiroAgua() {
        Tabuleiro t = new Tabuleiro(10);
        ResultadoTiro r = t.atirar(new Coordenada(5, 5));
        assertEquals(ResultadoTiro.Tipo.AGUA, r.getTipo());
    }

    @Test
    void tiroAcerto() {
        Tabuleiro t = new Tabuleiro(10);
        Navio n = new Navio(0, "Destroyer", 2);
        t.posicionarNavio(n, new Coordenada(3, 3), true, "NONE");
        ResultadoTiro r = t.atirar(new Coordenada(3, 3));
        assertEquals(ResultadoTiro.Tipo.ACERTO, r.getTipo());
    }

    @Test
    void tiroAfundou() {
        Tabuleiro t = new Tabuleiro(10);
        Navio n = new Navio(0, "Destroyer", 2);
        t.posicionarNavio(n, new Coordenada(0, 0), true, "NONE");
        t.atirar(new Coordenada(0, 0));
        ResultadoTiro r = t.atirar(new Coordenada(1, 0));
        assertEquals(ResultadoTiro.Tipo.AFUNDOU, r.getTipo());
        assertNotNull(r.getNavioAfundado());
        assertEquals("Destroyer", r.getNavioAfundado().getNome());
    }

    @Test
    void tiroJaAtirado() {
        Tabuleiro t = new Tabuleiro(10);
        t.atirar(new Coordenada(0, 0));
        ResultadoTiro r = t.atirar(new Coordenada(0, 0));
        assertEquals(ResultadoTiro.Tipo.JA_ATIRADO, r.getTipo());
    }

    // ===== Fim de jogo =====

    @Test
    void todosAfundados_quandoNenhum() {
        Tabuleiro t = new Tabuleiro(10);
        // sem navios: considera verdadeiro (stream vazia → allMatch = true)
        assertTrue(t.todosAfundados());
    }

    @Test
    void todosAfundados_aposAfundarTodos() {
        Tabuleiro t = new Tabuleiro(10);
        Navio n = new Navio(0, "Destroyer", 2);
        t.posicionarNavio(n, new Coordenada(0, 0), true, "NONE");
        t.atirar(new Coordenada(0, 0));
        t.atirar(new Coordenada(1, 0));
        assertTrue(t.todosAfundados());
    }

    @Test
    void naoAfundados_navioVivo() {
        Tabuleiro t = new Tabuleiro(10);
        Navio n = new Navio(0, "Destroyer", 2);
        t.posicionarNavio(n, new Coordenada(0, 0), true, "NONE");
        t.atirar(new Coordenada(0, 0)); // apenas 1 dos 2
        assertFalse(t.todosAfundados());
    }

    // ===== ValidadorDeFrota =====

    @Test
    void validadorFrotaCorreta() {
        int[] tamanhos = {5, 4, 3, 3, 2};
        Tabuleiro t = new Tabuleiro(10);
        int id = 0;
        int[] cols  = {0, 0, 0, 0, 0};
        int[] rows  = {0, 2, 4, 6, 8};
        for (int i = 0; i < tamanhos.length; i++) {
            Navio n = new Navio(id++, "N" + i, tamanhos[i]);
            t.posicionarNavio(n, new Coordenada(cols[i], rows[i]), true, "NONE");
        }
        ValidadorDeFrota v = new ValidadorDeFrota(10, tamanhos);
        ValidadorDeFrota.Resultado r = v.validar(t);
        assertTrue(r.ok(), "Erros: " + r.erros());
    }

    @Test
    void validadorFrotaQuantidadeErrada() {
        Tabuleiro t = new Tabuleiro(10);
        Navio n = new Navio(0, "N", 2);
        t.posicionarNavio(n, new Coordenada(0, 0), true, "NONE");
        ValidadorDeFrota v = new ValidadorDeFrota(10, new int[]{5, 4, 3, 3, 2});
        ValidadorDeFrota.Resultado r = v.validar(t);
        assertFalse(r.ok());
        assertFalse(r.erros().isEmpty());
    }
}
