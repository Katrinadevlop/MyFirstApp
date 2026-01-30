# Решение заданий по Coroutines

## Задание №1: Remove & likes

### Реализованные методы

#### PostRepository (интерфейс)
```kotlin
suspend fun likeById(id: Long)
suspend fun removeById(id: Long)
```

#### PostRepositoryHybridImpl (реализация)
Оба метода следуют логике:
1. **Сначала модифицируем/удаляем запись в локальной БД** - это обеспечивает быстрый отклик UI
2. **Затем отправляем запрос в API**
3. **При успехе** - обновляем БД данными с сервера
4. **При ошибке** - откатываем изменения в БД и пробрасываем исключение

**Особенности реализации:**
- Сохраняем состояние до изменения для возможности отката
- Обрабатываем как ошибки HTTP (неуспешный response), так и сетевые ошибки (IOException)
- Логируем все ошибки для отладки

#### PostViewModel
```kotlin
fun likeById(id: Long)
fun removeById(id: Long)
```

Методы выполняются в отдельном потоке с использованием `runBlocking` для вызова suspend-функций репозитория. При ошибке устанавливают флаг `error = true` в `FeedModel`.

### Обработка ошибок
При возникновении ошибки (сетевой или серверной):
- Изменения откатываются в локальной БД
- Устанавливается `feedState.error = true`
- UI может показать кнопку Retry через наблюдение за `feedState`

---

## Задание №2: Save* (Оптимистичное сохранение)

### Изменения в модели данных

#### PostEntity и Post
Добавлены новые поля:
```kotlin
val isSynced: Boolean = true  // true - сохранен на сервере, false - только локально
val localId: Long? = null     // Временный локальный ID для несинхронизированных постов
```

#### База данных
- Версия БД увеличена до 3
- Используется `fallbackToDestructiveMigration()` для упрощения миграции

### Логика оптимистичного сохранения

#### Метод `add()` в PostRepositoryHybridImpl
1. **Создаем локальную версию поста:**
   - `id = 0` (Room автоматически сгенерирует ID)
   - `isSynced = false`
   - `localId = System.currentTimeMillis()` (уникальный временный ID)

2. **Сохраняем в локальную БД** - пост сразу появляется в RecyclerView

3. **Отправляем на сервер:**
   - При успехе: удаляем локальную версию, сохраняем с ID от сервера и `isSynced = true`
   - При ошибке: пост остается в БД с флагом `isSynced = false`

### Повторная синхронизация

#### PostDao
```kotlin
@Query("SELECT * FROM posts WHERE isSynced = 0")
suspend fun getUnsyncedPosts(): List<PostEntity>
```

#### PostRepository
```kotlin
fun retrySyncUnsavedPosts(onSuccess: () -> Unit, onError: (Exception) -> Unit)
```

Метод:
1. Получает все несинхронизированные посты из БД
2. Для каждого поста пытается отправить на сервер
3. При успехе - заменяет локальную версию на серверную
4. Логирует результаты для каждого поста

#### PostViewModel
```kotlin
fun retrySyncUnsavedPosts()
```

Вызывает метод репозитория с обработкой состояний loading/error.

### Визуальная индикация (рекомендации для UI)

В адаптере можно использовать поле `post.isSynced` для:
- Показа иконки статуса (например, часы для несинхронизированных постов)
- Блокировки действий (лайки, шаринг) для несинхронизированных постов
- Изменения стиля карточки (полупрозрачность, другой цвет фона)

Пример в ViewHolder:
```kotlin
if (!post.isSynced) {
    // Показываем иконку "ожидание синхронизации"
    syncStatusIcon.visibility = View.VISIBLE
    // Блокируем кнопку лайка
    likeButton.isEnabled = false
} else {
    syncStatusIcon.visibility = View.GONE
    likeButton.isEnabled = true
}
```

### Использование Retry

При ошибке синхронизации:
1. UI показывает Snackbar или диалог с кнопкой "Retry"
2. По нажатию вызывается `viewModel.retrySyncUnsavedPosts()`
3. Метод пытается синхронизировать все несохраненные посты

---

## Тестирование

### Сценарии для проверки:

1. **Лайк с сетью:**
   - Нажать лайк → должен мгновенно отобразиться
   - Проверить, что изменения сохранились на сервере

2. **Лайк без сети:**
   - Отключить сеть в эмуляторе
   - Нажать лайк → должен отобразиться, затем откатиться
   - Должна показаться ошибка

3. **Удаление с сетью:**
   - Удалить пост → должен исчезнуть
   - Проверить, что удален с сервера

4. **Удаление без сети:**
   - Отключить сеть
   - Удалить пост → должен исчезнуть, затем вернуться
   - Должна показаться ошибка

5. **Создание поста без сети:**
   - Отключить сеть
   - Создать пост → должен появиться с индикатором "не синхронизирован"
   - Включить сеть, нажать Retry
   - Пост должен синхронизироваться и получить ID от сервера

---

## Преимущества реализации

### Задание №1
- ✅ Быстрый отклик UI (изменения применяются локально сразу)
- ✅ Надежный откат при ошибках
- ✅ Подробное логирование для отладки
- ✅ Обработка всех типов ошибок (сеть, сервер)

### Задание №2
- ✅ Посты появляются в UI мгновенно
- ✅ Пользователь может продолжать работу без ожидания ответа сервера
- ✅ Визуальная индикация статуса синхронизации
- ✅ Возможность повторной синхронизации всех несохраненных постов
- ✅ Не теряются данные при проблемах с сетью

---

## Файлы с изменениями

### Задание №1
- `app/src/main/java/ru/netology/nmedia/repository/PostPerository.kt`
- `app/src/main/java/ru/netology/nmedia/repository/PostRepositoryHybridImpl.kt`
- `app/src/main/java/ru/netology/nmedia/viewmodel/PostViewModel.kt`

### Задание №2
- `app/src/main/java/ru/netology/nmedia/dto/Post.kt`
- `app/src/main/java/ru/netology/nmedia/db/PostEntity.kt`
- `app/src/main/java/ru/netology/nmedia/db/PostDao.kt`
- `app/src/main/java/ru/netology/nmedia/db/AppDb.kt`
- `app/src/main/java/ru/netology/nmedia/repository/PostPerository.kt`
- `app/src/main/java/ru/netology/nmedia/repository/PostRepositoryHybridImpl.kt`
- `app/src/main/java/ru/netology/nmedia/viewmodel/PostViewModel.kt`
