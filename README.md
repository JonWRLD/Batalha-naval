# Batalha Naval 

**Projeto POO 2026-1**

---

## Sobre o projeto

Refatoração e evolução de um jogo de Batalha Naval monolítico para uma arquitetura orientada a objetos em Java. O projeto separa completamente domínio, UI e persistência, e adiciona:

- Validação de frota independente de interface
- Interface de terminal melhorada com dois tabuleiros lado a lado
- Persistência em banco de dados SQLite com histórico e replay de partidas
- Testes automatizados com JUnit 5

---

## Pré-requisitos

- Java 17 ou superior — https://adoptium.net
- Maven 3.6 ou superior — https://maven.apache.org/download.cgi

Para verificar se estão instalados:
```bash
java -version
mvn -version
```

---

## Estrutura do projeto

```
batalha-naval/
├── data/                        ← banco SQLite (gerado automaticamente)
└── src/
    ├── main/java/batalhanaval/
    │   ├── Main.java                        ← ponto de entrada
    │   ├── config/
    │   │   └── GameConfig.java              ← configurações do jogo
    │   ├── domain/
    │   │   ├── Coordenada.java              ← parsing e validação de coordenadas
    │   │   ├── Navio.java                   ← estado de cada navio
    │   │   ├── Tabuleiro.java               ← grade, posicionamento e tiros
    │   │   ├── ResultadoTiro.java           ← água / acerto / afundou
    │   │   ├── Jogador.java                 ← abstração de jogador
    │   │   ├── HumanPlayer.java             ← jogador humano
    │   │   ├── CpuPlayer.java               ← CPU (RANDOM / HUNT / PARITY)
    │   │   ├── Jogo.java                    ← fluxo de turnos e fim de jogo
    │   │   ├── Jogada.java                  ← registro de jogada
    │   │   └── ValidadorDeFrota.java        ← validação independente de UI
    │   ├── service/
    │   │   └── FrotaService.java            ← posicionamento da frota
    │   ├── persistence/
    │   │   └── GameRepository.java          ← todo o SQL centralizado aqui
    │   └── ui/
    │       └── TerminalUI.java              ← exibição e leitura do terminal
    └── test/java/batalhanaval/
        └── BatalhaNavalTest.java            ← testes JUnit 5
```

---

## Como jogar

Ao iniciar o programa aparece o menu principal:

```
[1] Nova partida
[2] Listar histórico de partidas
[3] Replay de uma partida
[X] Sair
```

### Nova partida
- Escolha posicionar sua frota **manualmente** ou **automaticamente**
- No posicionamento manual informe a coordenada inicial (ex: `A1`) e a direção (`H` ou `V`)
- Durante o jogo use o menu de turno:
  - `1` — Atirar
  - `2` — Ver log dos últimos eventos
  - `3` — Ver seu tabuleiro
  - `?` — Ajuda

### Coordenadas
- Colunas: letras de `A` a `J`
- Linhas: números de `1` a `10`
- Exemplo: `A1` (canto superior esquerdo), `J10` (canto inferior direito)

### Símbolos no tabuleiro
| Símbolo | Significado |
|---|---|
| `.` | Vazio / não atirado |
| `S` | Seu navio |
| `X` | Acerto |
| `o` | Água |

---

## Banco de dados

O banco SQLite é criado automaticamente em `data/batalha_naval.db`. Tabelas:

- **partidas** — id, inicio, fim, vencedor, seed
- **jogadores** — id, partida_id, nome, tipo
- **jogadas** — id, partida_id, turno, jogador, coordenada, resultado
- **frota_inicial** — posições iniciais dos navios (para replay completo)

---

## Testes

Os testes cobrem todas as regras do motor do jogo, sem terminal e sem banco:

- Coordenadas válidas e inválidas
- Posicionamento de navios (colisão, limites, adjacência)
- Tiros: água, acerto, afundou, já atirado
- Detecção de navio afundado
- Fim de jogo quando toda a frota é afundada
- Validação de frota completa

---

## Decisões de design

- **Domínio sem UI:** `Tabuleiro`, `Jogo`, `Navio` e `ValidadorDeFrota` nunca usam `System.out` ou `Scanner`
- **HumanPlayer usa callback:** a UI passa uma função ao construir o jogador; o domínio a chama sem saber que existe um terminal
- **SQL centralizado:** apenas `GameRepository` usa JDBC; domínio e UI não tocam em SQL
