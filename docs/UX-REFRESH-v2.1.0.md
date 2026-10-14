# Anotaí 2.1.0 — UX Refresh

Esta versão moderniza a experiência sem alterar o objetivo central do app: criar tarefas por linguagem natural e disparar avisos confiáveis.

## Interações principais

- deslize horizontal no calendário para trocar de mês;
- toque no nome do mês para escolher mês e ano;
- toque em **Hoje** para retornar à data atual;
- toque em **Nova tarefa** para abrir o compositor em Bottom Sheet;
- deslize uma tarefa para a direita para concluir/reabrir;
- deslize uma tarefa para a esquerda para excluir;
- use **Desfazer** no Snackbar após concluir ou excluir;
- toque em uma tarefa para ver detalhes, editar, concluir/reabrir ou excluir.

## Estados de tarefa

- **Próxima**: ocorrência ainda futura;
- **Passou do horário**: ocorrência já passou e não foi concluída;
- **Concluída**: marcada pelo usuário; alarmes dessa tarefa ficam cancelados até ela ser reaberta.

## Aparência

Em **Configurações > Aparência**:

- Sistema;
- Claro;
- Escuro;
- Material You / cores dinâmicas em Android 12 ou superior.

## Migração de dados

O banco passou para a versão 3 e adiciona a coluna `completed` com valor padrão `false`. A migração preserva as tarefas existentes.

## Checklist manual recomendado

1. abrir o app e confirmar que o compositor não aparece automaticamente;
2. trocar meses por gesto e pelas setas;
3. escolher mês/ano tocando no título;
4. criar tarefa em linguagem natural e confirmar a prévia;
5. concluir uma tarefa por swipe e conferir cancelamento de alertas;
6. usar **Desfazer** e conferir reagendamento;
7. excluir uma tarefa por swipe e usar **Desfazer**;
8. editar título, texto, data, horário e recorrência;
9. alternar Sistema/Claro/Escuro e Material You;
10. abrir Configurações, revisar diagnóstico, permissões e teste de alerta;
11. reiniciar o app e confirmar persistência das tarefas concluídas.
