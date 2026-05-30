package batalhanaval.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Valida uma frota independentemente de UI.
 * Retorna um resultado com ok e lista de mensagens de erro.
 */
public class ValidadorDeFrota {

    public record Resultado(boolean ok, List<String> erros) {}

    private final int boardSize;
    private final int[] fleetSizes;

    public ValidadorDeFrota(int boardSize, int[] fleetSizes) {
        this.boardSize = boardSize;
        this.fleetSizes = Arrays.copyOf(fleetSizes, fleetSizes.length);
    }

    public Resultado validar(Tabuleiro tabuleiro) {
        List<String> erros = new ArrayList<>();
        List<Navio> navios = tabuleiro.getNavios();

        // 1. Quantidade de navios
        if (navios.size() != fleetSizes.length) {
            erros.add("Esperados " + fleetSizes.length + " navios, encontrados " + navios.size() + ".");
        }

        // 2. Tamanhos corretos (em ordem)
        int[] tamanhosEncontrados = navios.stream()
                .mapToInt(Navio::getTamanho)
                .sorted()
                .toArray();
        int[] tamanhosEsperados = Arrays.copyOf(fleetSizes, fleetSizes.length);
        Arrays.sort(tamanhosEsperados);
        if (!Arrays.equals(tamanhosEncontrados, tamanhosEsperados)) {
            erros.add("Tamanhos da frota incorretos. Esperado: "
                    + Arrays.toString(tamanhosEsperados)
                    + ", encontrado: " + Arrays.toString(tamanhosEncontrados) + ".");
        }

        for (Navio n : navios) {
            List<Coordenada> celulas = n.getCelulas();

            // 3. Dentro do tabuleiro
            for (Coordenada c : celulas) {
                if (c.getCol() < 0 || c.getCol() >= boardSize
                        || c.getRow() < 0 || c.getRow() >= boardSize) {
                    erros.add("Navio " + n.getNome() + " possui célula fora do tabuleiro: " + c);
                }
            }

            // 4. Contínuo e reto (horizontal ou vertical, sem buracos)
            if (celulas.size() > 1) {
                boolean todosIguaisRow = celulas.stream().mapToInt(Coordenada::getRow).distinct().count() == 1;
                boolean todosIguaisCol = celulas.stream().mapToInt(Coordenada::getCol).distinct().count() == 1;

                if (!todosIguaisRow && !todosIguaisCol) {
                    erros.add("Navio " + n.getNome() + " não está em linha reta.");
                } else {
                    // Verifica ausência de buracos
                    if (todosIguaisRow) {
                        int[] cols = celulas.stream().mapToInt(Coordenada::getCol).sorted().toArray();
                        for (int i = 1; i < cols.length; i++) {
                            if (cols[i] != cols[i-1] + 1) {
                                erros.add("Navio " + n.getNome() + " tem buracos entre as células.");
                                break;
                            }
                        }
                    } else {
                        int[] rows = celulas.stream().mapToInt(Coordenada::getRow).sorted().toArray();
                        for (int i = 1; i < rows.length; i++) {
                            if (rows[i] != rows[i-1] + 1) {
                                erros.add("Navio " + n.getNome() + " tem buracos entre as células.");
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 5. Sobreposição (cruzando todos os pares)
        for (int i = 0; i < navios.size(); i++) {
            for (int j = i + 1; j < navios.size(); j++) {
                for (Coordenada c : navios.get(i).getCelulas()) {
                    if (navios.get(j).ocupaCelula(c)) {
                        erros.add("Navios " + navios.get(i).getNome()
                                + " e " + navios.get(j).getNome() + " se sobrepõem.");
                    }
                }
            }
        }

        return new Resultado(erros.isEmpty(), erros);
    }
}
