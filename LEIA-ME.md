# Anotaí — LEIA-ME

Este documento faz parte do desenvolvimento do aplicativo e deve ser atualizado sempre que uma nova forma de escrever, interpretar ou editar tarefas for adicionada.

## Fluxo obrigatório

O aplicativo nunca salva imediatamente depois de interpretar o texto.

1. Digite a anotação.
2. Toque em INTERPRETAR.
3. Confira os dados.
4. Escolha EDITAR, CANCELAR ou CONFIRMAR.
5. Somente CONFIRMAR salva e agenda os avisos.

## Avisos padrão

Toda tarefa com horário tem dois avisos:

- Notificação comum: 120 minutos antes (2 horas).
- Alerta forte: 60 minutos antes (1 hora).

O alerta forte usa som de alarme, vibração e tenta abrir em tela cheia quando o Android permite.

Se o texto contiver um alerta explícito, ele substitui o padrão de 60 minutos do alerta forte.

Exemplo:

`buscar filha amanhã 8h e alertar 30 minutos antes`

Resultado:

- notificação comum: 2 horas antes;
- alerta forte: 30 minutos antes.

Os dois tempos também podem ser alterados manualmente na edição.

## Exemplos básicos

- `buscar filha 8h`
- `fazer almoço 8h30`
- `buscar clientes 19h15`
- `dentista amanhã 14h`
- `consulta depois de amanhã 10h`
- `lanche amanhã 8 e meia`

## Horários

Exemplos aceitos:

- `8h`
- `8h30`
- `08:00`
- `14:15`
- `às 8`
- `8 e meia`

## Datas

Exemplos:

- `hoje`
- `amanhã`
- `depois de amanhã`
- `dia 10`
- `10/09`
- `10/09/2026`
- `dia 20 de junho`
- `terça que vem`

Datas incompletas podem gerar aviso de ambiguidade antes do salvamento.

## Repetição diária

- `tomar remédio todos os dias às 8h`
- `caminhar diariamente 7h`

## Repetição semanal

- `buscar clientes toda terça 20h`
- `academia segunda quarta e sexta 18h`
- `curso toda segunda 19h`

## Repetição mensal

- `pagar aluguel todo dia 5 às 9h`

## Data final de repetição

- `buscar clientes toda terça 20h até dia 20 de junho`
- `academia segunda quarta e sexta 18h até 20/12/2026`

## Formas de pedir alerta

- `alertar 1h antes`
- `alertar 1 hora antes`
- `alerta meia hora antes`
- `me avise 1h antes`
- `avisar 30 minutos antes`
- `com 30 minutos de antecedência`

## Edição antes de salvar

É possível alterar:

- nome da tarefa;
- data;
- horário;
- notificação comum em minutos;
- alerta forte em minutos;
- repetição;
- data final da repetição.

Depois da edição é obrigatório revisar e confirmar novamente.

## Tarefas salvas

Ao tocar numa tarefa é possível:

- ver todos os dados;
- ver o texto original completo;
- editar;
- excluir.

Editar exige nova confirmação.

Excluir cancela os alarmes da tarefa.

## Calendário

O calendário mensal mostra:

- tarefas únicas;
- tarefas diárias;
- tarefas semanais;
- tarefas mensais;
- recorrências até sua data final.

## Texto original

O texto digitado originalmente é preservado.

## Erros e ambiguidades

O aplicativo mantém o arquivo `erros-log.txt`.

Quando houver problema de interpretação, ambiguidade ou exceção relevante, o diagnóstico deve ser registrado.

O botão EXPORTAR ERROS compartilha esse arquivo.

## Alertas no Android

Dependendo do aparelho e versão do Android podem ser necessárias:

- permissão de notificações;
- alarmes exatos;
- permissão de tela cheia;
- permissões extras da Xiaomi/HyperOS.

Se a tela cheia for bloqueada pelo sistema, a notificação destacada continua funcionando como fallback.

## Notificação e alerta no mesmo texto

A notificação comum e o alerta forte podem ser configurados separadamente já na anotação.

Exemplos:

- `dentista amanhã 14h notificar 2h antes`
- `dentista amanhã 14h alertar 30 minutos antes`
- `dentista amanhã 14h notificar 3 horas antes e alertar 45 minutos antes`
- `reunião 18h notificação 2h antes e alerta meia hora antes`

Regras:

- `notificar`, `notifique` e `notificação` alteram a **notificação comum**;
- `alertar`, `alerta`, `me avise`, `avisar` e formas equivalentes já reconhecidas pelo app alteram o **alerta forte**;
- sem configuração explícita, continuam valendo:
  - notificação comum: 120 minutos antes;
  - alerta forte: 60 minutos antes;
- se a notificação comum ficar programada para depois do alerta forte, o app mostra um aviso antes da confirmação;
- se houver intenção de configurar uma notificação mas o tempo não for entendido, o app mantém 2 horas e mostra um aviso.

A tela de confirmação sempre mostra os dois tempos separadamente.

## Desativar ou usar somente um tipo de aviso

### Desativar alerta forte

Variações reconhecidas:

- `não alertar`
- `nao alertar`
- `n alertar`
- `não alerta`
- `sem alertar`
- `sem alerta`
- `sem alertas`
- `não quero alerta`
- `desativar alerta`
- `alerta desativado`

Exemplo:

`dentista amanhã 14h sem alerta`

Resultado:

- notificação comum: 2 horas antes;
- alerta forte: desativado.

### Desativar notificação

Variações reconhecidas:

- `não notificar`
- `nao notificar`
- `n notificar`
- `sem notificar`
- `sem notificação`
- `sem notificações`
- `sem notif`
- `sem noti`
- `não quero notificação`
- `desativar notificação`
- `notificação desativada`

### Somente alerta

Variações:

- `só alerta`
- `so alerta`
- `somente alerta`
- `apenas alerta`
- `só alertar`

Exemplo:

`buscar filha amanhã 8h só alerta e alertar 30 minutos antes`

Resultado:

- notificação comum: desativada;
- alerta forte: 30 minutos antes.

### Somente notificação

Variações:

- `só notificação`
- `so notificacao`
- `somente notificação`
- `apenas notificação`
- `só notificar`
- `só notif`
- `só noti`

Exemplo:

`reunião amanhã 18h só notificação e notificar 3 horas antes`

Resultado:

- notificação comum: 3 horas antes;
- alerta forte: desativado.

### Edição manual

Na tela EDITAR, os campos de aviso aceitam:

- `30`, `60`, `120` etc. para minutos antes;
- `0` para avisar exatamente no horário;
- `desativado` para desligar aquele aviso.

Na confirmação, aviso desligado aparece claramente como `Desativado`.

## Como as datas aparecem na tela

A data continua sendo salva normalmente, mas a interface usa um formato mais rápido de entender.

Regras:

- hoje: `Hoje | dom | 30.08.26`
- amanhã: `Amanhã | seg | 31.08.26`
- depois de amanhã: `Depois de amanhã | ter | 01.09.26`
- entre 3 e 7 dias à frente: `Próx. sáb | 05.09.26`
- mais distante: `qui | 26.11.26`
- data passada: `sáb | 29.08.26`

Abreviações:

- seg = segunda
- ter = terça
- qua = quarta
- qui = quinta
- sex = sexta
- sáb = sábado
- dom = domingo

`Próx.` só aparece quando a tarefa não é hoje, amanhã nem depois de amanhã e acontece em até 7 dias.

Esse formato é usado na confirmação, na lista e nos detalhes da tarefa.

## Navegação e botão Voltar

O botão ou gesto VOLTAR retorna para a tela anterior do aplicativo.

- calendário → tela inicial;
- tarefa aberta pelo calendário → volta para o calendário;
- tarefa aberta pela lista → volta para a lista;
- edição → volta para a tela anterior;
- confirmação de edição → volta para a edição;
- confirmação de exclusão → volta para os detalhes;
- LEIA-ME e PERMISSÕES → voltam para a tela anterior.

Na tela inicial, o primeiro VOLTAR mostra:

`Pressione voltar novamente para sair`

Somente o segundo toque, em até aproximadamente 2 segundos, fecha o aplicativo.

## Permissões automáticas

A tela inicial possui somente um botão visível:

`PERMISSÕES`

Sempre que uma tarefa é salva ou alterada, o aplicativo verifica automaticamente as permissões necessárias.

Ordem:

1. notificações;
2. notificações bloqueadas nas configurações;
3. alarmes exatos;
4. tela cheia;
5. revisão Xiaomi/HyperOS.

Quando o Android permite pedir diretamente, o app pede.

Alarmes exatos e tela cheia são acessos especiais do Android e podem exigir uma tela oficial do sistema.

O botão PERMISSÕES mostra o estado atual e permite corrigir o próximo item pendente.

### Avisos desativados na interface

Quando um aviso é desativado por frases como `sem alerta`, `não alertar`, `sem notificação` ou equivalentes, a interface mostra:

`Desativado`

O valor interno `-1` nunca é exibido ao usuário. Ele existe apenas internamente para indicar que aquele aviso não deve ser agendado.

Exemplo:

`buscar clientes toda terça 20h sem alerta até 30 de dezembro`

Na confirmação:

- alerta forte: `Desativado`;
- a data final continua sendo interpretada normalmente.

## Sugestões sutis para erros de digitação

O app tenta perceber palavras que parecem digitadas incorretamente.

Exemplo:

`sem alerya`

Pode aparecer discretamente acima do campo:

`alerya → alerta    SUBS.`

A palavra escrita pelo usuário não é alterada automaticamente.

Somente ao tocar em `SUBS.` o app substitui aquela ocorrência.

O sistema usa:

- o corretor ortográfico ativo do Android para palavras gerais;
- um vocabulário interno para palavras importantes do Anotaí, como alerta, notificação, dias da semana, meses e expressões de recorrência.

As sugestões são conservadoras: quando a confiança é baixa, nenhuma substituição é sugerida.

No máximo algumas sugestões são mostradas ao mesmo tempo para manter a interface discreta.

## Teste de alerta em 10 segundos

O teste continua disparando aproximadamente 10 segundos depois de acionado.

Quando dispara:

- usa áudio com finalidade de alarme;
- tenta tocar o som de alarme configurado no Android;
- usa um som de fallback se o toque não iniciar;
- vibra;
- mostra o volume atual do canal de alarme;
- possui botão `PARAR TESTE`;
- som e vibração param automaticamente em até 15 segundos.

Esse limite automático existe apenas no teste. O alerta forte real continua seguindo a regra normal de exigir que o usuário o encerre.

## Tema escuro

O Anotaí usa tema escuro em suas telas.

Isso inclui:

- tela inicial;
- calendário;
- detalhes;
- confirmação;
- edição;
- LEIA-ME;
- permissões;
- telas de teste e alerta.

## Palavra suspeita destacada no campo

Quando o app mostra uma sugestão com o botão `SUBS.`, a palavra correspondente dentro do próprio campo de texto fica:

- em negrito;
- riscada no meio.

Exemplo:

`sem alerya`

Enquanto estiver disponível:

`alerya → alerta    SUBS.`

a palavra `alerya` no campo fica em negrito e riscada.

Ao tocar em `SUBS.`, ela é substituída pela sugestão e o destaque desaparece.

## Nova tela Adicionar lembrete

A tela inicial não possui mais o campo de digitação nem o botão `INTERPRETAR`.

No lugar deles existe:

`ADICIONAR LEMBRETE`

Ao tocar, abre uma tela própria de criação.

Nessa tela:

- o campo de texto fica no topo;
- o texto é interpretado em tempo real;
- título, data, horário, notificação, alerta, repetição e data final são atualizados enquanto o usuário digita;
- avisos e ambiguidades aparecem em tempo real;
- não existe botão `INTERPRETAR`.

Quando terminar, o usuário toca em:

`REVISAR E CONFIRMAR`

A confirmação final obrigatória antes de salvar continua existindo.

### Sugestão contextual de erro

Exemplo:

`correr 6h, akerta 2h antes`

Se o app entender que `akerta` provavelmente significa `alerta`:

- `akerta 2h antes` fica vermelho, em negrito e riscado dentro do input;
- logo abaixo aparece discretamente:

`Você quis dizer alerta 2h antes?`

Ao tocar na frase, somente a palavra errada é substituída.

A correção nunca é aplicada silenciosamente.

### Sugestão por expressão inteira

`depois de amanhs`

deve gerar:

`Você quis dizer depois de amanhã?`

O trecho inteiro `depois de amanhs` fica vermelho, em negrito e riscado.

### Data em tempo real

A criação mostra datas no formato amigável:

- `Hoje | ter | 21.04.26`
- `Amanhã | qua | 22.04.26`
- `Depois de amanhã | qui | 23.04.26`
- `Próx. sáb | 25.04.26`
- `qui | 30.04.26`

## Versão exibida

A tela inicial mostra discretamente:

`versão 1.0.0.5`

O `versionName` do APK também é `1.0.0.5`.

Na tela `Adicionar lembrete`, o título intermediário `Interpretação em tempo real` foi removido. As informações continuam sendo atualizadas em tempo real, apenas sem esse texto na interface.


## Padrões de avisos

Por padrão, cada tarefa com data e horário recebe notificações em 2 horas antes, 30 minutos antes, 15 minutos antes, 5 minutos antes, 1 minuto antes e no momento da tarefa.

Por padrão, o alerta forte ocorre 1 hora antes e no momento da tarefa.

Comandos sem `só` ou `so` são adicionados aos padrões. Com `só`, `so`, `somente` ou `apenas`, ficam somente os horários escritos pelo usuário.

`alerta` e `alertar` são equivalentes. `notificar` e `notificação` também são equivalentes.

O alerta forte vibra por 15 segundos e depois toca por 30 segundos, inclusive quando a tela cheia não puder aparecer. As notificações comuns vibram.


## Regras atuais de avisos

Por padrão, cada tarefa recebe notificações em 2 horas antes, 30 minutos antes, 15 minutos antes, 5 minutos antes e no momento da tarefa.

Por padrão, o alerta forte ocorre 1 hora antes e no momento da tarefa.

Trinta minutos antes de cada alerta forte, o aplicativo envia um aviso específico. Esse aviso tem CANCELAR ALERTA, que cancela somente aquele alerta, e DISPENSAR, que remove apenas a notificação e mantém o alerta.

Horários que já passaram no momento em que a tarefa é criada são ignorados. Não existe disparo atrasado nem catch-up desses horários.

O compositor mostra ao vivo somente notificações, pré-alertas e alertas que ainda podem ser agendados.

O alerta forte nunca começa invisível. Primeiro o aplicativo publica uma interface visível e cancelável. Depois começa 15 segundos de vibração e então 30 segundos de toque. Se não houver permissão para mostrar a notificação cancelável, o alerta forte não inicia som ou vibração invisíveis.

### Edição de tarefas salvas

Ao editar uma tarefa, o campo **Texto original** também pode ser alterado. Os horários de **Notificação** e **Alerta forte** mostrados na edição são calculados pela mesma política usada no agendamento real. Assim, a tela não oferece mais um único campo numérico que poderia divergir da lista efetivamente programada.

Depois de revisar a edição, a tela de confirmação continua obrigatória. Somente **CONFIRMAR ALTERAÇÕES** salva a mudança e reagenda os avisos.

Comandos exclusivos seguem o significado literal: `só alerta` desativa notificações comuns e mantém os alertas padrão; `só notificação` desativa alertas fortes e mantém as notificações padrão. Quando o horário é escrito, por exemplo `só alertar 2h antes`, somente esse horário de alerta é usado.

Na tela de confirmação, **Notificação**, **Alerta forte** e **Aviso antes do alerta** agora mostram apenas horários que ainda podem realmente disparar para a próxima ocorrência. Horários que já passaram no dia são omitidos, usando a mesma política do scheduler.

## Estado da v1

A primeira versão estável do **Anotaí** foi fechada sem IA local. O núcleo da v1 inclui interpretação de tarefas em texto, confirmação antes de salvar, calendário, edição, recorrência, notificações múltiplas, pré-alertas canceláveis, alertas fortes, reagendamento, persistência e restauração após reinicialização.

As telas de confirmação, cartões e detalhes usam a mesma política de horários efetivamente futuros. Tarefas novas e editadas também são normalizadas pela mesma política antes de serem persistidas.

A integração de **IA local fica planejada para uma versão posterior** e não faz parte desta v1.
