# Anotaí

baixe [aqui](https://github.com/fabioqueiroz1415/anotai/releases/download/v2.0.0/anotai-v2.0.0.apk) o apk.

O **Anotaí** é um aplicativo Android para criar lembretes escrevendo de forma natural.

Exemplo:

> buscar minha filha amanhã 18h e me alertar 1h antes

O aplicativo interpreta a data, o horário e a antecedência do alerta. Antes de salvar, mostra as informações entendidas para você conferir.

## Recursos

- criação de tarefas em texto natural;
- calendário de tarefas;
- notificações e alertas;
- tarefas recorrentes;
- edição e exclusão;
- confirmação antes de salvar.

## Instalar o APK normalmente

Para uma pessoa que só quer usar o Anotaí, não é necessário Android Studio nem compilar o projeto.

Basta ter o arquivo `Anotai.apk` no telefone, tocar nele e seguir a instalação do Android.

Na primeira instalação, o Android pode pedir autorização para **instalar aplicativos desconhecidos** para o aplicativo usado para abrir o APK. Essa autorização é controlada pelo próprio Android.

## Enviar e instalar automaticamente pelo computador

O projeto também contém o arquivo:

```text
celular.sh
```

Ele serve para quem já tem um APK pronto e quer colocá-lo no telefone automaticamente pelo cabo USB.

O script **não compila o projeto**.

Ele faz automaticamente:

1. procura um APK já gerado;
2. detecta o telefone conectado;
3. copia o APK para `Download/Anotai` no telefone;
4. instala ou atualiza o Anotaí;
5. abre o aplicativo.

### Uso normal

Se você acabou de compilar pelo Gradle, o script encontra automaticamente o APK dentro de `app/build/outputs/apk/`.

Também é possível simplesmente colocar o APK na pasta principal do projeto com este nome:

```text
Anotai.apk
```

Depois execute:

```bash
./celular.sh
```

Não é necessário informar caminho, versão, IP ou qualquer outro parâmetro.

### Configuração do telefone na primeira vez

Para a instalação automática por USB, o Android precisa permitir a comunicação com o computador.

Faça isto uma única vez:

1. conecte o telefone ao computador usando um cabo USB de dados;
2. ative **Opções do desenvolvedor** no Android;
3. dentro das opções do desenvolvedor, ative **Depuração USB**;
4. mantenha o telefone desbloqueado;
5. quando aparecer **Permitir depuração USB?**, toque em **Permitir**;
6. execute novamente `./celular.sh`.

Se o telefone ainda não estiver pronto, o próprio script mostra essas instruções e permite tentar novamente.

### ADB

O envio automático usa o ADB, ferramenta oficial de comunicação com dispositivos Android.

Se o ADB não estiver instalado no Ubuntu/Debian, o `celular.sh` oferece a instalação automaticamente. Também pode ser instalado manualmente com:

```bash
sudo apt install adb
```

### Proteção dos dados

O script atualiza o aplicativo usando a instalação existente e **nunca desinstala o Anotaí automaticamente**.

Se o APK tiver uma assinatura diferente da versão já instalada, o processo é interrompido. Isso evita que uma desinstalação automática apague as tarefas armazenadas no telefone.

## Gerar o APK

Para desenvolvimento, o APK de debug pode ser criado com:

```bash
./gradlew assembleDebug
```

Normalmente ele será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Depois disso basta executar:

```bash
./celular.sh
```

## Tecnologias

- Kotlin
- Jetpack Compose
- Android
- SQLite

## Versão

**v1.0.0**
