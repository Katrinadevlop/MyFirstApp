# NMedia — Миграция на Room

Проект переведён на Room. Приложение хранит посты и черновик в SQLite через Room, сохраняя прежнюю функциональность.

## Что сделано
- Подключён Room (runtime, ktx, kapt) — см. `app/build.gradle.kts`.
- База данных: `AppDb` (`@Database(entities = [PostEntity, DraftEntity], version = 1)`).
- Сущности: `PostEntity`, `DraftEntity`.
- DAO: `PostDao`, `DraftDao`.
- Репозиторий: `PostRepositoryRoomImpl` используется из `PostViewModel`.
- Инициализация БД начальными данными: `App.kt` — однократно читает `posts.json` (если он есть) и наполняет таблицу, когда она пуста.
- Удалены неиспользуемые реализации репозитория: in-memory и file.

## Проверка
1. Сборка
   - Windows PowerShell:
     ```
     .\gradlew.bat assembleDebug
     ```
   - macOS/Linux:
     ```
     ./gradlew assembleDebug
     ```
2. Ручной прогон:
   - Открытие списка постов
   - Лайк/шер/просмотр
   - Создание нового поста
   - Редактирование/удаление поста
   - Автосохранение и очистка черновика

## Где что лежит
- БД и DAO: `app/src/main/java/ru/netology/nmedia/db/`
- Репозиторий: `app/src/main/java/ru/netology/nmedia/repository/PostRepositoryRoomImpl.kt`
- ViewModel’ы: `app/src/main/java/ru/netology/nmedia/viewmodel/`

## Публикация в GitHub (выполните сами)
1. Создайте пустой репозиторий на GitHub.
2. В корне проекта выполните:
   ```
   git init
   git add .
   git commit -m "Migrate to Room"
   git branch -M main
   git remote add origin https://github.com/<YOUR_USERNAME>/<REPO>.git
   git push -u origin main
   ```
3. Отправьте ссылку на репозиторий в личном кабинете Нетологии.
