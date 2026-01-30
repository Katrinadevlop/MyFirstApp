# Задание №1: New Posts - Реализация

## Описание
Реализована фоновая загрузка новых постов через метод `getNewer`, которые не отображаются сразу в RecyclerView. Вместо этого появляется плашка (Snackbar) с уведомлением о новых постах, при нажатии на которую происходит плавный скролл к началу списка и отображение загруженных постов.

## Реализованные изменения

### 1. API Layer
**Файл:** `app/src/main/java/ru/netology/nmedia/api/PostApiService.kt`
- ✅ Добавлен метод `getNewer(@Path("id") id: Long): Response<List<Post>>`
- Запрашивает посты новее указанного ID с endpoint: `GET /api/posts/{id}/newer`

### 2. Database Layer
**Файл:** `app/src/main/java/ru/netology/nmedia/db/PostDao.kt`
- ✅ Добавлен метод `getMaxId(): Long?` для получения максимального ID поста из БД
- SQL запрос: `SELECT MAX(id) FROM posts`

### 3. Repository Layer

#### PostRepository (интерфейс)
**Файл:** `app/src/main/java/ru/netology/nmedia/repository/PostPerository.kt`
- ✅ Добавлен `suspend fun getNewer(currentMaxId: Long): List<Post>`
- ✅ Добавлен `suspend fun getMaxPostId(): Long`
- ✅ Добавлен `suspend fun saveNewerPosts(posts: List<Post>)`

#### PostRepositoryHybridImpl
**Файл:** `app/src/main/java/ru/netology/nmedia/repository/PostRepositoryHybridImpl.kt`
- ✅ Реализован `getNewer()` - запрашивает новые посты с сервера, НЕ сохраняя их в БД
- ✅ Реализован `getMaxPostId()` - получает максимальный ID из локальной БД
- ✅ Реализован `saveNewerPosts()` - сохраняет скрытые посты в БД

#### Другие репозитории
- ✅ `PostRepositoryNetworkImpl` - добавлены заглушки
- ✅ `PostRepositoryRoomImpl` - добавлены заглушки
- ✅ `PostRepositoryOkHttpImpl` - добавлены заглушки

### 4. ViewModel Layer
**Файл:** `app/src/main/java/ru/netology/nmedia/viewmodel/PostViewModel.kt`

Добавлено:
- ✅ `LiveData<Int> newerPostsCount` - счетчик скрытых новых постов
- ✅ `loadNewerPosts()` - загружает новые посты в фоне
- ✅ `showNewerPosts()` - сохраняет скрытые посты в БД и сбрасывает счетчик
- ✅ `startBackgroundLoading()` - запускает периодическую загрузку (каждые 10 секунд)
- ✅ `onCleared()` - останавливает фоновую загрузку при уничтожении ViewModel

### 5. UI Layer
**Файл:** `app/src/main/java/ru/netology/nmedia/ui/FeedFragment.kt`
- ✅ Подписка на `newerPostsCount` из ViewModel
- ✅ Показ Snackbar при появлении новых постов
- ✅ При нажатии на Snackbar:
  - Вызывается `viewModel.showNewerPosts()`
  - Выполняется `binding.list.smoothScrollToPosition(0)`

**Характеристики Snackbar:**
- Длительность: `LENGTH_INDEFINITE` (не исчезает автоматически)
- Action button: "Показать"
- Автоматически скрывается при нажатии или когда счетчик = 0

### 6. Resources
**Файлы:** 
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ru/strings.xml`

Добавлены строки:
- ✅ `new_posts_available` - "Новые посты" / "Новые посты доступны"
- ✅ `show_new_posts` - "Показать"

### 7. Gradle (исправление конфликта)
**Файл:** `client/build.gradle.kts`
- ✅ Обновлена версия Kotlin plugin с 1.9.10 на 2.2.20 для совместимости

## Архитектурные решения

### 1. Оптимизация запросов
- Вместо запроса всех постов, `getNewer` запрашивает только посты с `id > maxId`
- `maxId` получается из локальной БД через `SELECT MAX(id)`
- Это значительно снижает нагрузку на сеть и сервер

### 2. Периодическая загрузка
- Используется `Handler.postDelayed()` с интервалом 10 секунд
- Фоновая загрузка запускается в `init` блоке ViewModel
- Останавливается в `onCleared()` для предотвращения утечек памяти

### 3. Material Design
- Согласно гайдлайнам Material Design используется **Snackbar** с action button
- `LENGTH_INDEFINITE` гарантирует, что уведомление не исчезнет до действия пользователя

### 4. UX
- Новые посты не отображаются автоматически (не отвлекают пользователя)
- Плавный скролл `smoothScrollToPosition(0)` создает приятную анимацию
- Snackbar автоматически скрывается при сбросе счетчика

## Как это работает

1. **Фоновая загрузка:** Каждые 10 секунд ViewModel запрашивает новые посты
2. **Проверка ID:** Запрос отправляется с максимальным ID из локальной БД
3. **Кэширование:** Новые посты сохраняются в памяти ViewModel (не в БД)
4. **Уведомление:** Обновляется LiveData `newerPostsCount`, что триггерит показ Snackbar
5. **Действие пользователя:** При нажатии на "Показать":
   - Новые посты сохраняются в БД
   - RecyclerView автоматически обновляется (LiveData из Room)
   - Происходит плавный скролл к началу
   - Snackbar исчезает

## Тестирование

Для тестирования функционала:

1. Запустите приложение
2. На сервере добавьте новый пост (через API или другое устройство)
3. Через 10 секунд появится Snackbar с уведомлением
4. Нажмите "Показать" - должен произойти плавный скролл к новому посту

## Возможные улучшения

1. Добавить возможность изменения интервала загрузки
2. Остановка фоновой загрузки когда приложение в фоне
3. Показ количества новых постов в Snackbar (например: "5 новых постов")
4. Добавить звуковое/вибро уведомление при появлении новых постов
5. Сохранение позиции скролла при показе новых постов
