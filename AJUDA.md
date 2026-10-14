# Anotaí — Ajuda

O Anotaí transforma frases simples em tarefas com data, horário, recorrência, notificações e alertas. A versão 2.1.0 mantém a interpretação em linguagem natural e deixa o uso diário mais rápido, principalmente no calendário.

## Criar uma tarefa

Na tela principal, toque em **Nova tarefa** e escreva como você falaria normalmente.

Exemplos:

- `buscar filha amanhã 8h`
- `fazer almoço 8h30`
- `dentista sexta 14h`
- `buscar clientes toda terça 20h até dia 20 de junho`
- `academia segunda quarta e sexta 18h`
- `dentista amanhã 14h alertar 30 minutos antes`

Enquanto você digita, o Anotaí mostra uma prévia do que entendeu. Confira os dados e toque em **Salvar tarefa**.

O texto original é preservado para consulta e edição.

## Calendário

A tela inicial usa um calendário mensal.

- Deslize para a esquerda para avançar um mês.
- Deslize para a direita para voltar um mês.
- Toque no nome do mês para escolher diretamente o mês e o ano.
- Use **Hoje** para retornar rapidamente à data atual.
- Toque em um dia para ver suas tarefas.
- Pequenos pontos abaixo do número indicam que há tarefas naquele dia.
- O dia selecionado e o dia atual têm destaques diferentes para facilitar a orientação.

A data selecionada aparece em linguagem natural, como **Hoje, 3 de setembro** ou **Quinta-feira, 3 de setembro**.

## Estados das tarefas

As tarefas podem aparecer como:

- **Próxima**: ainda está por acontecer.
- **Passou do horário**: o horário já passou e a tarefa não foi concluída.
- **Concluída**: foi marcada como finalizada.

Tarefas concluídas ficam visualmente atenuadas e não continuam gerando novos alarmes. Uma tarefa concluída pode ser reaberta; nesse caso, os próximos avisos são programados novamente quando aplicável.

## Gestos nas tarefas

Nos cards de tarefa:

- deslize para a **direita** para concluir;
- em uma tarefa concluída, deslize para a **direita** para reabrir;
- deslize para a **esquerda** para excluir.

Depois de excluir, o Anotaí oferece **Desfazer** por uma Snackbar, evitando perda acidental.

Tocar no card abre os detalhes completos.

## Detalhes e edição

Na tela de detalhes você pode consultar:

- título;
- data e horário;
- recorrência;
- notificações;
- alertas;
- data final da recorrência, quando houver;
- texto original digitado.

Também é possível:

- editar a tarefa;
- concluir ou reabrir;
- excluir.

Ao editar, o agendamento anterior é substituído pelo novo para evitar alarmes duplicados.

## Datas e horários reconhecidos

Exemplos de horário:

- `8h`
- `8h30`
- `08:00`
- `14:15`
- `às 8`
- `8 e meia`

Exemplos de data:

- `hoje`
- `amanhã`
- `depois de amanhã`
- `dia 10`
- `10/09`
- `10/09/2026`
- `dia 20 de junho`
- `terça que vem`

Quando uma informação é ambígua, confira a prévia antes de salvar.

## Recorrência

Exemplos:

- `tomar remédio todos os dias às 8h`
- `caminhar diariamente 7h`
- `buscar clientes toda terça 20h`
- `academia segunda quarta e sexta 18h`
- `pagar aluguel todo dia 5 às 9h`
- `buscar clientes toda terça 20h até dia 20 de junho`

O calendário mostra as ocorrências geradas pela recorrência até a data final definida, quando existir.

## Notificações e alertas

Por padrão, quando a tarefa tem horário, o Anotaí programa notificações comuns em:

- 2 horas antes;
- 30 minutos antes;
- 15 minutos antes;
- 5 minutos antes;
- no horário da tarefa.

Também programa alertas fortes em:

- 1 hora antes;
- no horário da tarefa.

O alerta forte usa som, vibração e tenta abrir em tela cheia quando o Android permite.

Você pode acrescentar horários explícitos no próprio texto. Exemplos:

- `dentista amanhã 14h notificar 3 horas antes`
- `dentista amanhã 14h alertar 30 minutos antes`
- `reunião 18h notificar 2h antes e alertar meia hora antes`

Os tempos explícitos são incorporados às regras reconhecidas pelo aplicativo. A prévia mostra o resultado antes do salvamento.

## Desativar um tipo de aviso

Exemplos reconhecidos incluem:

- `dentista amanhã 14h sem alerta`
- `dentista amanhã 14h não alertar`
- `reunião 18h sem notificação`
- `reunião 18h não notificar`

Também é possível pedir somente um tipo de aviso usando expressões como **somente alertar** ou **somente notificar**.

## Aparência

Em **Configurações > Aparência**, escolha:

- **Sistema**: acompanha o modo claro/escuro do aparelho;
- **Claro**;
- **Escuro**.

Em Android 12 ou superior, o **Material You** pode usar as cores dinâmicas do aparelho. Essa opção pode ser ativada ou desativada nas Configurações.

## Permissões e diagnóstico

Em **Configurações**, a seção de confiabilidade mostra o estado das permissões necessárias para alarmes e notificações.

Dependendo do aparelho e da versão do Android, podem ser necessárias permissões para:

- notificações;
- alarmes exatos;
- tela cheia;
- execução confiável em segundo plano;
- ajustes específicos do fabricante, como Xiaomi/HyperOS.

Se a abertura em tela cheia for bloqueada pelo sistema, a notificação destacada continua sendo usada como alternativa.

A mesma área permite:

- revisar permissões;
- testar um alerta;
- exportar informações de diagnóstico/erros;
- abrir esta ajuda.

## Privacidade e armazenamento

As tarefas são armazenadas localmente pelo aplicativo no aparelho. O Anotaí não precisa enviar o conteúdo das suas tarefas para um serviço remoto para interpretar os textos suportados pelo app.

## Dicas para obter uma interpretação melhor

- Inclua um horário quando quiser receber avisos.
- Use uma data explícita quando a tarefa não for para hoje.
- Para recorrências, escreva claramente os dias ou a frequência.
- Se houver data final, use `até ...`.
- Confira a prévia antes de salvar, especialmente quando a frase tiver mais de uma data ou horário.

## Versão

Esta ajuda descreve o **Anotaí 2.1.0**.
