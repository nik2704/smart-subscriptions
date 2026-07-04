# Умный реестр подписок

Полный backend-проект на Java 21 + Spring Boot + Gradle для учёта подписок и регулярных платежей. Все комментарии и пояснения в проекте написаны на русском языке.

## Что реализовано

- CRUD-ядро для обязательств и истории оплат.
- PostgreSQL + Flyway-миграции без ручного создания таблиц.
- Swagger UI по адресу `/docs`.
- SSE-канал `/obligations/events` для события удаления.
- Логика lazy expiry.
- Оплата рекуррентных и разовых обязательств.
- Автоматический расчёт следующих дат без накопления ошибки на границах месяцев.
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

### Через Docker Compose

```bash
docker compose up --build
```

После старта:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/docs`
- PostgreSQL: `localhost:5432`

Миграции Flyway применяются автоматически при запуске приложения, потому что `spring.flyway.enabled=true`, а контейнер приложения стартует только после `healthcheck` базы.

### Локально без Docker

1. Поднять PostgreSQL.
2. Создать базу `subscriptions`.
3. Задать переменные окружения:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/subscriptions
export SPRING_DATASOURCE_USERNAME=subscriptions
export SPRING_DATASOURCE_PASSWORD=subscriptions
```

4. Запустить приложение:

```bash
./gradlew bootRun
# или
gradle bootRun
```

## Тесты

```bash
./gradlew test
# или
gradle test
```

Тесты не требуют реальной базы и внешних сервисов: репозитории и интеграции замоканы через Mockito.

## Почему lazy expiry не применяется к рекуррентным обязательствам

Lazy expiry переводит в `expired` только разовые обязательства, у которых дата уже прошла и `recurrence = null`.

Причина в бизнес-смысле рекуррентной подписки: если дата следующего списания уже наступила, это ещё не означает прекращение сервиса. На практике пользователь может не отметить оплату вовремя, письмо о списании может прийти позже, а AI-модуль может дообогатить данные постфактум. Если автоматически переводить такую запись в `expired`, интерфейс начнёт показывать действующую подписку как завершённую, что исказит реестр и сломает сценарии напоминаний.

Поэтому для рекуррентных подписок просроченная дата означает только то, что нужно зафиксировать оплату через `/obligations/{id}/pay`, а не то, что услуга завершилась.

## Обработка граничных случаев с датами

Сдвиг новой даты считается **от текущего `next_payment_date`, а не от момента оплаты**. Это защищает систему от накопления смещения при просроченных оплатах.

Примеры:

- `2025-01-31 + monthly = 2025-02-28`
- `2024-02-29 + yearly = 2025-02-28`
- `2026-07-31 + quarterly = 2026-10-31`

В реализации используется стандартная календарная арифметика Java `LocalDate.plusMonths(...)` и `LocalDate.plusYears(...)`. Она корректно обрабатывает конец месяца и високосные годы, то есть работает как аналог `relativedelta` для данного кейса.

## Компромиссы

1. В проекте выбран Flyway как аналог Alembic для экосистемы Spring Boot, потому что он нативно интегрируется со стеком Java.
2. Тесты реализованы как unit/web-slice, а не как полноценные интеграционные с Testcontainers, потому что по условию внешних зависимостей быть не должно.
3. AI-интеграция показана как готовая точка расширения на уровне API и бизнес-правил, но без подключения конкретной LLM или почтового провайдера. Это позволяет держать ядро домена чистым.
4. Для SSE сделан простой in-memory publisher. Для одного инстанса этого достаточно, но в production с несколькими репликами лучше вынести события в брокер, например Redis Pub/Sub или Kafka.

## Что сделал бы иначе при большем времени

- Добавил бы интеграционные тесты с Testcontainers в отдельном профиле.
- Ввёл бы аудит истории изменений обязательств через отдельную таблицу или outbox-паттерн.
- Разделил бы доменную и persistence-модель при дальнейшем росте проекта.
- Добавил бы идемпотентность для запросов от AI-модуля по внешнему ключу письма или чека.
- Подключил бы security, multi-user tenancy и rate limiting.

## API

### POST /obligations

Создаёт обязательство.

Особенности:

- Если дата в прошлом, запись создаётся со статусом `EXPIRED`.
- Если уже есть активное обязательство с тем же title без учёта регистра, запись всё равно создаётся, но ответ содержит `warning`.

Пример:

```bash
curl -X POST http://localhost:8080/obligations \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Яндекс.Плюс",
    "amount": 399.00,
    "currency": "RUB",
    "category": "SUBSCRIPTION",
    "recurrence": "MONTHLY",
    "nextPaymentDate": "2026-07-10"
  }'
```

### GET /obligations

Фильтрация:

```bash
curl 'http://localhost:8080/obligations?category=SUBSCRIPTION&status=ACTIVE'
```

### GET /obligations/upcoming?days=7

```bash
curl 'http://localhost:8080/obligations/upcoming?days=7'
```

### POST /obligations/{id}/pay

```bash
curl -X POST http://localhost:8080/obligations/11111111-1111-1111-1111-111111111111/pay
```

### PATCH /obligations/{id}/cancel

```bash
curl -X PATCH http://localhost:8080/obligations/11111111-1111-1111-1111-111111111111/cancel
```

### DELETE /obligations/{id}

```bash
curl -X DELETE http://localhost:8080/obligations/11111111-1111-1111-1111-111111111111
```

### SSE

```bash
curl -N http://localhost:8080/obligations/events
```

После удаления приходит событие:

```json
{"type":"obligation_deleted","id":"..."}
```

## Структура проекта

```text
src/main/java/com/example/subscriptions
├── config
├── controller
├── domain
├── dto
├── exception
├── mapper
├── repository
├── service
└── sse
```
