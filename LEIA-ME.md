# Couchcraft + Legacy4J Bridge

*[Read in English](README.md)*

Mod Fabric que faz a tela dividida local do [Couchcraft](https://github.com/zortos293/Couchcraft) funcionar com o [Legacy4J](https://github.com/Wilyicaro/Legacy-Minecraft), deixando o modo cooperativo no sofá parecido com as edições de console: uma janela só, Start em outro controle para entrar e todos os jogadores com a experiência completa do Legacy4J.

Feito para o modpack **Re-Console** no Minecraft **26.3** (Fabric).

> **Aviso sincero:** não sou desenvolvedor Java. O mod foi escrito com ajuda de IA (Claude) e testado por mim durante muitas horas. Algumas partes são remendos: o Legacy4J guarda o estado de um jogador só em objetos globais, e este mod troca esse estado por jogador usando reflection. Funciona, mas pode quebrar quando os mods atualizarem. O arquivo [docs/UPSTREAM.md](docs/UPSTREAM.md) explica o que deixaria isso limpo.

## Recursos

**Controles**
- O Legacy4J cuida do controle de todos os jogadores: layout de botões do console, navegação nos menus pelo D-pad e analógico, remapeamento, ícones do controle e cursor virtual no inventário.
- Cada jogador tem o próprio estado do controle; os comandos de um não vazam para o outro.
- O controle do jogador 1 não pode ser tomado por outro controle (Back num controle livre não faz nada).

**Entrar e sair (estilo console)**
- Start num controle livre mostra "Pressione START novamente para entrar" no meio da tela; Start de novo abre a seleção de perfis.
- "Sair do jogo" no menu de um convidado mostra "Sair da sessão" em vez de "voltar ao menu principal", e só tira aquele jogador.
- Segurar Back + Start continua tirando o convidado (Couchcraft).

**HUD e visual por jogador**
- Layout de HUD do Legacy4J para os convidados.
- Timers do HUD, paper doll (o boneco do canto ao correr, agachar ou voar), dicas de botão e corrida com toque duplo são separados por jogador.
- Sem nome em cima do próprio boneco no inventário e na paper doll.

**Skins**
- Cada perfil do Couchcraft guarda a própria skin e os favoritos do Legacy Skins (`config/couchlegacy/guest_skins.json`).

**Menus dos convidados (como no console)**
- Convidados acessam: Continuar, Ajuda e Opções (Trocar skin, Como jogar, Controles), Placares, Conquistas, Sair e a lista de jogadores (Select) com o chat.
- Só o jogador 1: Configurações (gráficos, áudio, idioma, pacotes), Mods, Salvar jogo, Opções do anfitrião, privilégios dos jogadores e Abrir para LAN.
- Com cheats ligados no mundo, todos os jogadores podem usar comandos.

**Configurações dentro dos menus do Legacy4J (como na edição de Nintendo Switch)**
- *Ajuda e Opções → Configurações → Opções de Jogo*: **Tela dividida na vertical**.
- *Ajuda e Opções → Configurações → Interface de Usuário*: **Tamanho do HUD (tela dividida)**, usado no lugar do Tamanho do HUD enquanto a tela está dividida.
- A tela de configurações do próprio Couchcraft fica em *Ajuda e Opções → Mods → Couchcraft*.

**Correções**
- Crash quando um convidado entra num mundo com a borda do mundo visível (por exemplo, o Tutorial).
- Crash enquanto o convidado ainda escolhe o perfil.
- Tradução em português das telas do Couchcraft.

## Requisitos

Minecraft 26.3, Fabric Loader 0.19.5 ou mais novo, Couchcraft 0.2.x, Legacy4J 1.9.x e Java 25.

**Incompatíveis:** Sodium (o Couchcraft não suporta), RenderScale (todas as telas mostravam a mesma câmera) e BedrockSkins (a sincronização de skin dele só conhece um jogador).

## Instalação

Baixe o jar em Releases e coloque na pasta `mods`, junto com o Couchcraft e o Legacy4J.

## Como compilar

1. Instale o JDK 25.
2. Crie uma pasta `mods` ao lado do `build.gradle` e copie para ela o `couchcraft-0.2.0.jar` e o `Legacy4J-26.3-1.9.1-fabric.jar` (ou use `-PmodsDir="caminho/da/sua/pasta/mods"`).
3. Rode `gradlew build`. O jar sai em `build/libs/`.

## Créditos

- [Couchcraft](https://github.com/zortos293/Couchcraft), de zortos293 (MIT): o motor da tela dividida.
- [Legacy4J](https://github.com/Wilyicaro/Legacy-Minecraft), de Wilyicaro e colaboradores (LGPL-3.0): a experiência de console.
- Ponte por FensZard, escrita com o Claude (Anthropic).

Licença MIT, veja [LICENSE](LICENSE).
