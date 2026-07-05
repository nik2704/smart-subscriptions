# Backend реестр подписок

Backend-проект на Java 21 + Spring Boot + Gradle для учёта подписок и регулярных платежей.

## Что реализовано

- CRUD для обязательств и истории оплат.
- PostgreSQL + Flyway-миграции без ручного создания таблиц.
- Swagger UI по адресу `/docs`.
- SSE-канал `/obligations/events` для события удаления.
- Логика lazy expiry.
- Оплата рекуррентных и разовых обязательств.
- Автоматический расчёт следующих дат.
- Docker Compose запуск одной командой.
- Unit и WebMvc тесты без внешних зависимостей.

## Стек

- Java 21
- Spring Boot 3
- Gradle
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- springdoc-openapi
- JUnit 5 + Mockito

## Запуск

Для запуска необходимо обеспечить:
1. Поднять PostgreSQL.
2. доступ Postgres по стандартному порту (`localhost:5432`)
3. Создать базу `subscriptions`.
```sql
CREATE USER subscriptions WITH PASSWORD 'subscriptions';
CREATE DATABASE subscriptions OWNER subscriptions;
GRANT ALL PRIVILEGES ON DATABASE subscriptions TO subscriptions;
```

### Запуск Через Docker Compose
Необходимый образ скачивается с dh-mirror.gitverse.ru (Зеркало GitHub), т.е. необходим доступ к dh-mirror.gitverse.ru.

Для запуска backend выполнить команду:
```bash
docker compose up --build
```


### Удалить образ
Посмотреть контейнеры
```docker ps```

Остановить нужный контейнер
```docker stop smart-subscriptions-app```

Удалить контейнер
```docker rm smart-subscriptions-app```

Удалить образ приложения
```docker rmi smart-subscriptions-app```

### Локально без Docker

1. Задать переменные окружения:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/subscriptions
export SPRING_DATASOURCE_USERNAME=subscriptions
export SPRING_DATASOURCE_PASSWORD=subscriptions
```

2. Запустить приложение:

```bash
chmod +x gradlew
./gradlew bootRun
```

## После старта:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/docs`
- Миграции Flyway применяются автоматически при запуске приложения, потому что `spring.flyway.enabled=true`, а контейнер приложения стартует только после `healthcheck` базы.

## Тесты

```bash
./gradlew test
```

Тесты не требуют реальной базы и внешних сервисов: репозитории и интеграции - через Mockito.

## Почему lazy expiry не применяется к рекуррентным обязательствам

Lazy expiry переводит в `expired` только разовые обязательства, у которых дата уже прошла и `recurrence = null`.

Причина: если дата следующего списания уже наступила, это ещё не означает прекращение сервиса. Пользователь может не отметить оплату вовремя, письмо о списании может прийти позже, а AI-модуль может дополнить данные позже.
Если автоматически переводить такую запись в `expired`, интерфейс начнёт показывать действующую подписку как завершённую, что может сломать сценарии напоминаний.

Т.е. для рекуррентных подписок просроченная дата - необходимость зафиксировать оплату через `/obligations/{id}/pay`, а не то, что услуга завершилась.

## Обработка граничных случаев с датами

Сдвиг новой даты считается **от текущего `next_payment_date`, а не от момента оплаты** (используется `LocalDate.plusMonths(...)` и `LocalDate.plusYears(...)`).
Это защищает систему от накопления смещения при просроченных оплатах.

Примеры:
- `2025-01-31 + monthly = 2025-02-28`
- `2024-02-29 + yearly = 2025-02-28`
- `2026-07-31 + quarterly = 2026-10-31`

## Компромиссы

1. Тесты реализованы как unit/web-slice, а не как полноценные интеграционные (без внешних зависимостей).
2. Нет взаимодействия с AI.
3. Для SSE сделан простой in-memory publisher. Для одного инстанса этого достаточно, но в production с несколькими репликами лучше вынести события в брокер, например Redis Pub/Sub или Kafka.

## Что сделал бы иначе при большем времени

- Добавил бы интеграционные тесты.
- Ввёл бы аудит истории изменений обязательств через отдельную таблицу.
- Добавил бы взаимодействие с AI-модулем.
- Подключил бы security.

## Postman

В репозитории находятся готовые файлы для быстрого тестирования API в Postman:

- `postman_collection.json` — коллекция запросов
- `postman_environment.json` — окружение с переменными

### Импорт в Postman

1. Открыть Postman.
2. Выбрать **Import**.
3. Выбрать файлы:
    - `postman_collection.json`
    - `postman_environment.json`
4. После импорта - переключить окружение из `postman_environment.json`.

### Что проверить после импорта

В окружении корректно заполнены переменные:
- `baseUrl` — базовый URL API, например `http://localhost:8080`
- при необходимости — токены, идентификаторы и другие переменные коллекции

### Использование

После выбора окружения можно запускать запросы из коллекции по порядку:
- создание обязательства;
- получение списка обязательств;
- получение upcoming-обязательств;
- оплата обязательства;
- отмена обязательства;
- удаление обязательства.

Если приложение запущено локально:
```text
baseUrl=http://localhost:8080
```





