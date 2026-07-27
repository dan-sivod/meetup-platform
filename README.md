# Meetup Platform

Мультисервисная платформа «встречи друзей» на Kotlin + Spring Boot 3.
Каждый сервис владеет своими данными (database-per-service), интеграция —
синхронно через API Gateway и асинхронно через Kafka.

## Сервисы

| Сервис | Порт | БД | Назначение |
|---|---|---|---|
| gateway | 8080 | — | Единая точка входа, JWT-валидация, маршрутизация |
| auth-service | 8081 | auth_db | Регистрация, логин, JWT access/refresh |
| users-service | 8082 | users_db | Профили (создаются по событию `user.registered`) |
| friends-service | 8083 | friends_db | Заявки в друзья, блокировки, ACL-проверка |
| meetups-service | 8084 | meetups_db | Агрегат «встреча»: инвайты, RSVP, confirm/cancel |
| scheduling-service | 8085 | scheduling_db | Опросы по слотам времени, голосование |
| places-service | 8086 | places_db | Каталог мест, geo-поиск (haversine), избранное |
| chat-service | 8087 | chat_db | Комнаты встреч (по `meetup.created`), WebSocket/STOMP |
| notifications-service | 8088 | notifications_db | In-app inbox, проекция всех доменных событий |
| media-service | 8089 | media_db | Фото встреч (локальный диск; в проде — S3 presigned) |
| recommendations-service | 8090 | in-memory | Скоринг мест по favorites и подтверждённым встречам |
| search-service | 8091 | in-memory | Индекс людей/встреч/мест (в проде — OpenSearch) |

## События Kafka

`user.registered`, `friendship.requested`, `friendship.accepted`,
`meetup.created`, `meetup.confirmed`, `meetup.cancelled`, `rsvp.changed`,
`message.sent`, `place.created`, `place.favorited` — контракты в модуле `common`.

## Запуск

Требования: JDK 21, Docker.

```bash
# 1. Инфраструктура (PostgreSQL + Kafka)
docker compose up -d

# 2. Сборка
./gradlew build

# 3. Запуск сервисов (каждый в своём терминале, либо через IDE)
./gradlew :services:auth-service:bootRun
./gradlew :services:users-service:bootRun
# ... остальные по аналогии
./gradlew :services:gateway:bootRun
```

## Пример сценария (через gateway)

```bash
# Регистрация → токены
curl -s localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"a@example.com","password":"password1","displayName":"Alice"}'

# Дальше все вызовы с Authorization: Bearer <accessToken>
# Заявка в друзья
curl -s localhost:8080/api/friendships -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"addresseeId":"<uuid>"}'

# Создать встречу с инвайтами (только друзья)
curl -s localhost:8080/api/meetups -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Настолки в субботу","invitedUserIds":["<uuid>"]}'

# RSVP
curl -s -X PUT localhost:8080/api/meetups/<id>/rsvp \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"GOING"}'

# Подтвердить время и место (host)
curl -s localhost:8080/api/meetups/<id>/confirm \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"startsAt":"2026-08-01T18:00:00Z","placeId":null}'

# Inbox уведомлений
curl -s localhost:8080/api/notifications -H "Authorization: Bearer $TOKEN"
```

## Архитектурные решения

- **Доверенный заголовок `X-User-Id`** — gateway валидирует JWT и проставляет
  заголовок; клиентский `X-User-Id` всегда отбрасывается. Сервисы за gateway
  доверяют заголовку (в проде — плюс mTLS/network policy).
- **ACL инвайтов** — meetups-service синхронно спрашивает friends-service
  (`GET /friendships/check`), приглашать можно только друзей.
- **Чат-комнаты и уведомления** создаются асинхронно по событиям, write-path
  встречи не блокируется.
- **Search/Reco** держат in-memory проекции и восстанавливаются перечитыванием
  топиков (`auto-offset-reset: earliest`); замена на OpenSearch/ML-ранкер не
  меняет контракты.
- `ddl-auto: update` и упрощённые хранилища — осознанные упрощения для
  демо-стенда; в проде: Flyway-миграции, PostGIS для geo, S3 для медиа.

## Упрощения относительно целевой архитектуры

- `message.sent` не создаёт уведомлений (нет presence-сервиса, чтобы понять,
  кто офлайн).
- Rate limiting, refresh-token rotation, Redis-сессии — не реализованы.
- Медиа хранится на локальном диске без модерации.
