# Anotaí

O **Anotaí** é um aplicativo Android que eu fiz para anotar tarefas escrevendo do jeito normal.

Exemplo:

> buscar minha filha amanhã 18h e me alertar 1h antes

O aplicativo tenta entender a data, o horário e os alertas. Antes de salvar, ele mostra uma tela para eu conferir se entendeu certo.

## O que tem no app

- tarefas escritas em texto;
- calendário;
- notificações e alertas;
- tarefas que se repetem;
- edição e exclusão;
- confirmação antes de salvar.

## Imagens

### Tela inicial

Colocar aqui: `docs/imagens/01-tela-inicial.png`

<!-- ![Tela inicial](docs/imagens/01-tela-inicial.png) -->

### Confirmação da tarefa

Colocar aqui: `docs/imagens/02-confirmacao.png`

<!-- ![Confirmação](docs/imagens/02-confirmacao.png) -->

### Calendário

Colocar aqui: `docs/imagens/03-calendario.png`

<!-- ![Calendário](docs/imagens/03-calendario.png) -->

### Alerta

Colocar aqui: `docs/imagens/04-alerta.png`

<!-- ![Alerta](docs/imagens/04-alerta.png) -->

## Tecnologias

- Kotlin
- Jetpack Compose
- Android
- SQLite

## Como gerar o APK

```bash
./gradlew assembleDebug
```

O APK fica em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Versão

**v1.0.0**
