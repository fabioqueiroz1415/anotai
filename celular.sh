#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_NAME="com.anotai.app"
LAUNCH_ACTIVITY="com.anotai.app/.SplashActivity"
REMOTE_DIR="/sdcard/Download/Anotai"

info()  { printf '\033[1;34m%s\033[0m\n' "$*"; }
ok()    { printf '\033[1;32m✓ %s\033[0m\n' "$*"; }
warn()  { printf '\033[1;33m! %s\033[0m\n' "$*"; }
error() { printf '\033[1;31m✗ %s\033[0m\n' "$*" >&2; }

pause() {
    printf '\n'
    read -r -p "Pressione ENTER para tentar novamente ou digite q para sair: " answer
    [[ "${answer,,}" != "q" ]] || exit 0
}

find_apk() {
    local -a candidates=()

    # Caminho mais simples: o usuário pode colocar o APK ao lado deste script.
    [[ -f "$ROOT_DIR/Anotai.apk" ]] && candidates+=("$ROOT_DIR/Anotai.apk")
    [[ -f "$ROOT_DIR/anotai.apk" ]] && candidates+=("$ROOT_DIR/anotai.apk")

    # Também procura APKs já gerados pelo Gradle. O script NÃO compila o projeto.
    while IFS= read -r -d '' apk; do
        [[ "$apk" == *"-unsigned.apk" ]] && continue
        [[ "$apk" == *"androidTest"* ]] && continue
        candidates+=("$apk")
    done < <(find "$ROOT_DIR/app/build/outputs/apk" -type f -name '*.apk' -print0 2>/dev/null || true)

    if [[ ${#candidates[@]} -eq 0 ]]; then
        return 1
    fi

    # Remove duplicados e escolhe o APK mais recente.
    printf '%s\n' "${candidates[@]}" \
        | awk '!seen[$0]++' \
        | while IFS= read -r apk; do
            printf '%s\t%s\n' "$(stat -c '%Y' "$apk" 2>/dev/null || printf '0')" "$apk"
          done \
        | sort -nr \
        | head -n 1 \
        | cut -f2-
}

install_adb_if_possible() {
    if command -v adb >/dev/null 2>&1; then
        return 0
    fi

    warn "O programa ADB ainda não está instalado neste computador."
    printf 'Ele é necessário apenas para enviar e instalar o APK pelo cabo USB.\n\n'

    if command -v apt-get >/dev/null 2>&1; then
        read -r -p "Quer instalar o ADB agora? [S/n]: " answer
        answer="${answer:-s}"
        if [[ "${answer,,}" == "s" || "${answer,,}" == "sim" ]]; then
            sudo apt-get install -y adb
        fi
    fi

    if ! command -v adb >/dev/null 2>&1; then
        error "ADB não encontrado."
        printf 'No Ubuntu/Debian, instale com:\n\n  sudo apt install adb\n\n'
        exit 1
    fi
}

list_authorized_devices() {
    adb devices | awk 'NR > 1 && $2 == "device" {print $1}'
}

list_unauthorized_devices() {
    adb devices | awk 'NR > 1 && $2 == "unauthorized" {print $1}'
}

choose_device() {
    local -a devices=()
    local -a unauthorized=()
    local serial model choice

    while true; do
        mapfile -t devices < <(list_authorized_devices)
        mapfile -t unauthorized < <(list_unauthorized_devices)

        if [[ ${#devices[@]} -eq 1 ]]; then
            printf '%s\n' "${devices[0]}"
            return 0
        fi

        if [[ ${#devices[@]} -gt 1 ]]; then
            printf '\nMais de um Android foi encontrado:\n' >&2
            for i in "${!devices[@]}"; do
                serial="${devices[$i]}"
                model="$(adb -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || true)"
                printf '  %d) %s%s\n' "$((i + 1))" "$serial" "${model:+ - $model}" >&2
            done

            read -r -p "Escolha o número do aparelho: " choice
            if [[ "$choice" =~ ^[0-9]+$ ]] && (( choice >= 1 && choice <= ${#devices[@]} )); then
                printf '%s\n' "${devices[$((choice - 1))]}"
                return 0
            fi

            warn "Escolha inválida."
            continue
        fi

        if [[ ${#unauthorized[@]} -gt 0 ]]; then
            warn "O telefone foi encontrado, mas ainda não autorizou este computador."
            printf '\nNo telefone:\n'
            printf '  1. Desbloqueie a tela.\n'
            printf '  2. Procure a mensagem “Permitir depuração USB?”.\n'
            printf '  3. Toque em “Permitir”.\n'
            pause
            continue
        fi

        warn "Nenhum telefone Android foi encontrado."
        printf '\nFaça esta configuração uma única vez:\n'
        printf '  1. Conecte o telefone ao computador com um cabo USB de dados.\n'
        printf '  2. No Android, ative “Opções do desenvolvedor”.\n'
        printf '  3. Ative “Depuração USB”.\n'
        printf '  4. Desbloqueie o telefone e aceite “Permitir depuração USB?”.\n'
        pause
    done
}

printf '\n========================================\n'
printf '       Anotaí - instalar no celular\n'
printf '========================================\n\n'

APK_PATH="$(find_apk || true)"

if [[ -z "$APK_PATH" ]]; then
    error "Não encontrei nenhum APK pronto."
    printf '\nColoque o APK na mesma pasta de celular.sh com o nome:\n\n'
    printf '  Anotai.apk\n\n'
    printf 'Depois execute novamente:\n\n'
    printf '  ./celular.sh\n\n'
    exit 1
fi

ok "APK encontrado: $(basename "$APK_PATH")"
info "Este script usa o APK pronto; ele não recompila o projeto."

install_adb_if_possible
adb start-server >/dev/null

SERIAL="$(choose_device)"
MODEL="$(adb -s "$SERIAL" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || true)"
ok "Telefone encontrado: ${MODEL:-Android}"

APK_NAME="Anotai.apk"
REMOTE_APK="$REMOTE_DIR/$APK_NAME"

info "Enviando o Anotaí para o telefone..."
adb -s "$SERIAL" shell mkdir -p "$REMOTE_DIR" >/dev/null
adb -s "$SERIAL" push "$APK_PATH" "$REMOTE_APK" >/dev/null
ok "APK copiado para Download/Anotai/$APK_NAME"

info "Instalando/atualizando o aplicativo..."
INSTALL_OUTPUT="$(adb -s "$SERIAL" install -r "$APK_PATH" 2>&1 || true)"

if ! grep -q 'Success' <<<"$INSTALL_OUTPUT"; then
    if grep -Eq 'INSTALL_FAILED_UPDATE_INCOMPATIBLE|INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES' <<<"$INSTALL_OUTPUT"; then
        error "A versão instalada foi assinada com uma chave diferente deste APK."
        warn "Nada foi desinstalado, para proteger as tarefas salvas no telefone."
        printf '\nSe você realmente quiser substituir essa instalação, faça backup dos dados antes de remover o app manualmente.\n'
    else
        error "Não foi possível instalar o APK."
        printf '\nMensagem retornada pelo Android/ADB:\n%s\n' "$INSTALL_OUTPUT"
    fi
    exit 1
fi

ok "Anotaí instalado/atualizado"

info "Abrindo o Anotaí..."
adb -s "$SERIAL" shell am force-stop "$PACKAGE_NAME" >/dev/null 2>&1 || true
if adb -s "$SERIAL" shell am start -n "$LAUNCH_ACTIVITY" >/dev/null 2>&1; then
    ok "Anotaí aberto no telefone"
else
    warn "O aplicativo foi instalado, mas não consegui abri-lo automaticamente."
    printf 'Abra o Anotaí normalmente pelo ícone do telefone.\n'
fi

printf '\n========================================\n'
ok "Tudo pronto!"
printf 'O APK também ficou salvo em Download/Anotai no telefone.\n'
printf '========================================\n\n'
