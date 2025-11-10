# Push-Sender (FCM)

CLI-утилита для отправки push-уведомлений в формате, совместимом с Android-приложением.

Формат data-сообщения:
- action: "NEW_POST"
- content: JSON-строка вида { "userName": string, "postText": string }

Требования
- Node.js >= 18
- Сервисный ключ Firebase Admin SDK (не коммитить!). Укажите путь в переменной окружения GOOGLE_APPLICATION_CREDENTIALS.

Установка
```bash
npm i
```

Отправка нового поста
```bash
# Windows PowerShell (пример)
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\service-account.json"
node index.js new-post --token "<DEVICE_FCM_TOKEN>" --userName "Иван Иванов" --postText "Текст поста\nв несколько строк"

# macOS/Linux
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"
node index.js new-post --token "<DEVICE_FCM_TOKEN>" --userName "Иван Иванов" --postText "Текст поста\nв несколько строк"
```

Замечания
- Ключ сервисного аккаунта и токены устройств — секреты. Не размещайте их в репозитории.
- Формат полностью совместим с FirebaseService в Android-проекте.
