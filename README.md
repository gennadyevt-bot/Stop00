# STOP VPN v4.0.0

Android VPN-клиент на базе **AmneziaWG** (ранее WireGuard). Чёрная тема, красная кнопка STOP, русский интерфейс.

---

## Содержание

1. [Описание](#описание)
2. [Технологии](#технологии)
3. [Структура проекта](#структура-проекта)
4. [Настройка окружения](#настройка-окружения)
5. [Работа с репозиторием](#работа-с-репозиторием)
6. [GitHub Actions CI/CD](#github-actions-cicd)
7. [Смотреть логи CI](#смотреть-логи-ci)
8. [Скачать APK](#скачать-apk)
9. [Создать ссылку на APK](#создать-ссылку-на-apk)
10. [Менять иконки](#менять-иконки)
11. [Добавить сервер](#добавить-сервер)
12. [Серверная часть AmneziaWG](#серверная-часть-amneziawg)
13. [GitHub API](#github-api)
14. [История версий](#история-версий)

---

## Описание

STOP VPN — Android-приложение для подключения к VPN через протокол AmneziaWG (обфусцированный WireGuard). Тёмный интерфейс, главный экран с большой красной кнопкой STOP, список серверов, боковое меню, виджет, автоподключение.

**Особенности:**
- Движок AmneziaWG (com.zaneschepke:amneziawg-android:2.3.7)
- Параметры обфускации: Jc, Jmin, Jmax, S1, S2, H1–H4
- Чёрная тема, красная/зелёная кнопка
- Русский язык
- Боковое меню: резервное копирование, автоподключение, виджет, о приложении
- Встроенный демо-сервер

---

## Технологии

| Компонент | Версия |
|-----------|--------|
| compileSdk | 34 |
| minSdk | 24 |
| targetSdk | 34 |
| Kotlin | 1.9.23 |
| Gradle | 8.7 |
| AmneziaWG Android | 2.3.7 |
| Тема | Material3 DayNight.NoActionBar |

---

## Структура проекта

```
app/src/main/
├── AndroidManifest.xml
├── java/com/stopvpn/app/
│   ├── MainActivity.kt          # Главный экран
│   ├── VpnManager.kt            # Управление VPN
│   ├── ServerAdapter.kt         # Адаптер списка серверов
│   ├── ServerInfo.kt            # Модель сервера
│   ├── ServerStorage.kt         # Хранение серверов
│   ├── VpnStatus.kt             # Статусы VPN
│   ├── WgTunnel.kt              # Туннель AmneziaWG
│   └── ...
├── res/
│   ├── layout/
│   │   ├── activity_main.xml    # Главный экран
│   │   ├── item_server.xml      # Карточка сервера
│   │   ├── dialog_add_server.xml
│   │   ├── dialog_edit_server.xml
│   │   ├── dialog_menu.xml      # Боковое меню
│   │   └── widget_stop_vpn.xml  # Виджет
│   ├── values/
│   │   ├── colors.xml           # Цвета (красный #CC0000)
│   │   ├── strings.xml          # Русские строки
│   │   └── themes.xml           # Тёмная тема
│   ├── drawable/
│   │   ├── ic_logo_big.png      # Красный логотип
│   │   └── ic_logo_big_green.png # Зелёный логотип
│   └── mipmap-*/
│       ├── ic_launcher.png      # Иконка приложения
│       └── ic_launcher_round.png # Круглая иконка
└── assets/
    └── (конфиги серверов)
```

---

## Настройка окружения

### 1. Клонирование

```bash
git clone https://github.com/gennadyevt-bot/Stop00.git
cd Stop00
```

### 2. Android Studio

- Открыть проект
- Дождаться синхронизации Gradle
- Установить SDK 34

### 3. Локальная сборка

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## Работа с репозиторием

### Push через токен

1. Создать PAT на GitHub: Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Права: `repo` (полный доступ)
3. Использовать при пуше:

```bash
git remote set-url origin https://TOKEN@github.com/gennadyevt-bot/Stop00.git
git add .
git commit -m "Описание изменений"
git push origin main
```

### Изменение файлов через API

```bash
# Получить SHA
curl -s -H "Authorization: Bearer TOKEN" \
  "https://api.github.com/repos/gennadyevt-bot/Stop00/contents/app/src/main/res/values/colors.xml?ref=main"

# Обновить
curl -s -X PUT -H "Authorization: Bearer TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/gennadyevt-bot/Stop00/contents/app/src/main/res/values/colors.xml" \
  -d '{"message":"Обновлены цвета","content":"BASE64","sha":"SHA","branch":"main"}'
```

### Загрузка бинарных файлов

```bash
base64 -w 0 ic_launcher.png > icon.b64
# Затем PUT через API
```

---

## GitHub Actions CI/CD

### Workflow файл

`.github/workflows/android.yml`:

```yaml
name: Android CI
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      - name: Build Debug APK
        run: ./gradlew assembleDebug
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

### Запуск

CI запускается **автоматически** после push в `main`.

---

## Смотреть логи CI

### Через UI

1. Репозиторий → **Actions**
2. Выбрать запуск → job **build**
3. Раскрыть шаги, искать:
   - `e:` — ошибки Kotlin
   - `FAILURE` — падение сборки
   - `BUILD FAILED` — ошибка Gradle

### Через API

```bash
TOKEN="ghp_..."
REPO="gennadyevt-bot/Stop00"

# Список запусков
RUN_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "https://api.github.com/repos/$REPO/actions/runs?per_page=5" \
  | jq '.workflow_runs[0].id')

# Job'ы
JOB_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/jobs" \
  | jq '.jobs[0].id')

# Логи (ZIP)
curl -sL -H "Authorization: Bearer $TOKEN" \
  "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/logs" \
  -o logs.zip

unzip logs.zip
cat "build/6_Assemble Debug.txt"
```

---

## Скачать APK

### Через UI (Actions)

1. **Actions** → последний успешный запуск (✅)
2. Внизу **Artifacts** → **app-debug**
3. Скачается ZIP, внутри `app-debug.apk`

### Через API

```bash
# Получить artifact_id
ARTIFACT_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "https://api.github.com/repos/$REPO/actions/runs/RUN_ID/artifacts" \
  | jq '.artifacts[0].id')

# Скачать
curl -sL -H "Authorization: Bearer $TOKEN" \
  "https://api.github.com/repos/$REPO/actions/artifacts/$ARTIFACT_ID/zip" \
  -o artifact.zip

unzip artifact.zip
mv app-debug.apk StopVPN.apk
```

**Артефакты хранятся 90 дней.**

### Через GitHub Release (постоянная ссылка)

1. **Releases** → **Draft a new release**
2. Тег: `v4.0.0`
3. Загрузить APK → **Attach binaries**
4. Скопировать `browser_download_url` — прямая ссылка

---

## Создать ссылку на APK

### Временная ссылка (артефакт, 90 дней)

```
https://github.com/gennadyevt-bot/Stop00/actions/runs/RUN_ID/artifacts/ARTIFACT_ID
```

### Постоянная ссылка (Release)

```bash
# Создать релиз
curl -s -X POST -H "Authorization: Bearer TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/gennadyevt-bot/Stop00/releases" \
  -d '{"tag_name":"v4.0.1","name":"STOP VPN v4.0.1","body":"Обновление"}'

# Загрузить APK
curl -s -X POST -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/vnd.android.package-archive" \
  "UPLOAD_URL?name=StopVPN.apk" \
  --data-binary @app-debug.apk
```

### Формат ссылки для Kimi

В приложении Kimi на телефоне кликабельны только короткие ссылки с эмодзи:

```
[⬇ APK](https://github.com/.../StopVPN.apk)
```

Длинный markdown `[текст](url)` — **не кликабелен** в мобильном приложении.

---

## Менять иконки

### Исходник

Красная круглая кнопка STOP на чёрной подставке. Фотография крупным планом.

### Процесс

1. Обрезать — убрать статус-бар и панель навигации
2. Найти bbox объекта (яркость > 15)
3. Сделать квадрат с letterbox
4. Масштабировать до 1024x1024
5. Сгенерировать размеры:
   - mdpi: 48x48
   - hdpi: 72x72
   - xhdpi: 96x96
   - xxhdpi: 144x144
   - xxxhdpi: 192x192
6. Круглые версии через маску ellipse()
7. Загрузить в mipmap-*/ic_launcher.png и ic_launcher_round.png

### Python-скрипт

```python
from PIL import Image, ImageDraw

base = Image.open("stop_button_1024.png").convert("RGBA")
sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

for name, size in sizes.items():
    sq = base.resize((size, size), Image.LANCZOS)
    sq.save(f"ic_launcher_{name}.png")
    circle = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size, size), fill=255)
    circle.paste(sq, (0, 0), mask)
    circle.save(f"ic_launcher_round_{name}.png")
```

### Загрузка

```bash
# Получить SHA
curl -s -H "Authorization: Bearer TOKEN" \
  "https://api.github.com/repos/gennadyevt-bot/Stop00/contents/app/src/main/res/mipmap-hdpi/ic_launcher.png?ref=main"

# Обновить через PUT с base64
```

---

## Добавить сервер

### Через интерфейс

1. Нажать + (FAB) на главном экране
2. Ввести:
   - Название: NL-AMS-01
   - Страна: Нидерланды, Амстердам
   - Флаг: 🇳🇱
   - Конфигурацию AmneziaWG
3. Сохранить

### Встроить в код

```kotlin
val defaultServers = listOf(
    ServerInfo(
        id = "nl-ams-01",
        name = "NL-AMS-01",
        country = "Нидерланды, Амстердам",
        flagEmoji = "🇳🇱",
        config = "[Interface]\nPrivateKey = ...\nAddress = 10.0.0.2/24\nDNS = 1.1.1.1\n\n[Peer]\nPublicKey = ...\nPresharedKey = ...\nAllowedIPs = 0.0.0.0/0\nEndpoint = nl-ams-01.example.com:51820\nPersistentKeepalive = 25"
    )
)
```

### Параметры AmneziaWG

```
Jc = 4
Jmin = 40
Jmax = 70
S1 = 0
S2 = 0
H1 = 1
H2 = 2
H3 = 3
H4 = 4
```

---

## Серверная часть AmneziaWG

### Установка

```bash
sudo apt install amneziawg-tools
```

### Конфиг сервера

```ini
[Interface]
PrivateKey = SERVER_PRIVATE_KEY
Address = 10.0.0.1/24
ListenPort = 51820
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE
Jc = 4
Jmin = 40
Jmax = 70
S1 = 0
S2 = 0
H1 = 1
H2 = 2
H3 = 3
H4 = 4

[Peer]
PublicKey = CLIENT_PUBLIC_KEY
PresharedKey = PRESHARED_KEY
AllowedIPs = 10.0.0.2/32
```

### Запуск

```bash
sudo awg-quick up wg0
sudo systemctl enable awg-quick@wg0
```

### Ключи

```bash
awg genkey | tee privatekey | awg pubkey > publickey
awg genpsk > preshared
```

---

## GitHub API

### Эндпоинты

| Действие | Метод | URL |
|----------|-------|-----|
| Список запусков | GET | /repos/{REPO}/actions/runs |
| Job'ы | GET | /repos/{REPO}/actions/runs/{ID}/jobs |
| Логи | GET | /repos/{REPO}/actions/runs/{ID}/logs |
| Артефакты | GET | /repos/{REPO}/actions/runs/{ID}/artifacts |
| Скачать артефакт | GET | /repos/{REPO}/actions/artifacts/{ID}/zip |
| Содержимое файла | GET | /repos/{REPO}/contents/{PATH} |
| Обновить файл | PUT | /repos/{REPO}/contents/{PATH} |
| Коммиты | GET | /repos/{REPO}/commits |
| Создать релиз | POST | /repos/{REPO}/releases |

### Примеры

```bash
TOKEN="ghp_..."
REPO="gennadyevt-bot/Stop00"
HEADERS=(-H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github.v3+json")

# Последний коммит
curl -s "${HEADERS[@]}" "https://api.github.com/repos/$REPO/commits/main"

# Структура репозитория
curl -s "${HEADERS[@]}" "https://api.github.com/repos/$REPO/git/trees/main?recursive=1"

# Получить файл
curl -s "${HEADERS[@]}" "https://api.github.com/repos/$REPO/contents/app/build.gradle?ref=main"
```

---

## История версий

### v4.0.0
- Движок WireGuard → AmneziaWG
- Параметры обфускации Jc/Jmin/Jmax/S1/S2/H1-H4
- Поля AWG в диалогах
- Обновлены иконки (чистая обрезка, подставка сохранена)

### v3.1.5
- WireGuard, 31 файл, Kotlin, SDK 34, Gradle 8.7
- Демо: NL-AMS-01, DE-FRA-01, US-NYC-01

### v3.1.1
- Первая сборка, FrameLayout, красная/зелёная кнопка

---

## Ссылки

- Репозиторий: https://github.com/gennadyevt-bot/Stop00
- AmneziaWG Android: https://github.com/amnezia-vpn/amneziawg-android
- AmneziaWG Tools: https://github.com/amnezia-vpn/amneziawg-tools

---

*Документация актуальна на август 2026.*
