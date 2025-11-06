# ITMO-Market Backend

REST API маркетплейса для курса "Высоконагруженные системы" ИТМО.

## 📋 Таблица содержания

- [Технологический стек](#технологический-стек)
- [Требования](#требования)
- [Установка](#установка)
- [Запуск](#запуск)
- [API документация](#api-документация)
- [Архитектура базы данных](#архитектура-базы-данных)
- [Структура проекта](#структура-проекта)
- [Тестирование](#тестирование)
- [Развертывание](#развертывание)

## 🛠 Технологический стек

- **Язык**: Kotlin 1.9+
- **Framework**: Spring Boot 3.2+
- **Сборка**: Gradle (Kotlin DSL)
- **База данных**: PostgreSQL 15+
- **Миграции**: Flyway
- **Безопасность**: Spring Security + JWT
- **Документация**: OpenAPI 3.0 / Swagger
- **Тестирование**: JUnit 5, Testcontainers, MockK
- **Контейнеризация**: Docker + Docker Compose

## 📦 Требования

Перед началом убедитесь что у вас установлены:

- **Docker**: v20.10+
- **Docker Compose**: v1.29+
- **Git**: v2.30+
- **Java 17** (если запускать локально без Docker)
- **Gradle** (если собирать локально)

### Для локальной разработки без Docker:

```bash
# Требования
- Java 17 JDK
- PostgreSQL 15+
- Gradle 8.5+
```

## 🚀 Установка

### 1. Клонирование репозитория

```bash
git clone https://github.com/yourusername/itmo-market.git
cd itmo-market/backend
```

### 2. Подготовка переменных окружения

```bash
# Копируем файл .env
cp .env.example .env

# Редактируем переменные окружения при необходимости
nano .env  # или используйте ваш редактор
```

**Основные переменные окружения:**

| Переменная | Значение по умолчанию | Описание |
|---|---|---|
| `DB_HOST` | `postgres` | Хост базы данных |
| `DB_PORT` | `5432` | Порт PostgreSQL |
| `DB_NAME` | `itmo_market` | Название базы данных |
| `DB_USER` | `itmo_user` | Пользователь БД |
| `DB_PASSWORD` | `itmo_password` | Пароль пользователя БД |
| `JWT_SECRET` | *см. .env* | Секретный ключ JWT (минимум 256 бит) |
| `JWT_ACCESS_TOKEN_EXPIRATION` | `900000` | Время жизни access token (мс) - 15 минут |
| `JWT_REFRESH_TOKEN_EXPIRATION` | `604800000` | Время жизни refresh token (мс) - 7 дней |
| `SERVER_PORT` | `8080` | Порт приложения |

## 🎯 Запуск

### Вариант 1: Docker Compose (Рекомендуется)

```bash
# Запуск всех сервисов (PostgreSQL + Backend + Adminer)
docker-compose up -d

# Просмотр логов
docker-compose logs -f backend

# Остановка
docker-compose down

# Очистка (с удалением данных)
docker-compose down -v
```

### Вариант 2: Локальный запуск

```bash
# 1. Убедитесь что PostgreSQL запущена на localhost:5432
# 2. Обновите переменные окружения в application.yml

# 3. Сборка проекта
./gradlew build

# 4. Запуск приложения
./gradlew bootRun

# 5. Приложение будет доступно на http://localhost:8080
```

### Вариант 3: Build и запуск Docker образа вручную

```bash
# Build образа
docker build -t itmo-market-backend:latest .

# Запуск контейнера
docker run -d \
  --name itmo-market-backend \
  -p 8080:8080 \
  --env-file .env \
  itmo-market-backend:latest

# Просмотр логов
docker logs -f itmo-market-backend

# Остановка контейнера
docker stop itmo-market-backend
docker rm itmo-market-backend
```

## 📚 API документация

После запуска приложения API документация доступна по адресам:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

### Основные Endpoints

#### Аутентификация (`/api/auth`)

```
POST /api/auth/register           - Регистрация нового пользователя
POST /api/auth/login              - Вход в систему
POST /api/auth/refresh            - Обновление access token
```

#### Пользователи (`/api/users`)

```
GET  /api/users/me                - Получить профиль текущего пользователя
PUT  /api/users/me                - Обновить профиль
DELETE /api/users/me              - Удалить профиль
GET  /api/users/{id}              - Получить информацию о пользователе
```

#### Товары (`/api/products`)

```
GET  /api/products                - Список одобренных товаров (пагинация)
GET  /api/products/infinite       - Список товаров (infinite scroll)
GET  /api/products/search         - Поиск товаров по ключевым словам
GET  /api/products/{id}           - Получить товар
POST /api/products                - Создать товар
PUT  /api/products/{id}           - Обновить товар
DELETE /api/products/{id}         - Удалить товар
```

#### Магазины (`/api/shops`)

```
GET  /api/shops                   - Список магазинов
GET  /api/shops/{id}              - Получить магазин
GET  /api/shops/{id}/products     - Товары магазина
POST /api/shops                   - Создать магазин
PUT  /api/shops/{id}              - Обновить магазин
```

#### Корзина (`/api/cart`)

```
GET  /api/cart                    - Получить корзину
POST /api/cart/items              - Добавить товар в корзину
PUT  /api/cart/items/{itemId}     - Изменить количество товара
DELETE /api/cart/items/{itemId}   - Удалить товар из корзины
DELETE /api/cart                  - Очистить корзину
```

#### Заказы (`/api/orders`)

```
GET  /api/orders                  - Список заказов пользователя
GET  /api/orders/{id}             - Получить заказ
POST /api/orders                  - Оформить заказ из корзины
```

#### Комментарии (`/api/products/{productId}/comments`)

```
GET  /api/products/{id}/comments               - Список комментариев
POST /api/products/{id}/comments               - Добавить комментарий
PUT  /api/products/{id}/comments/{commentId}   - Обновить комментарий
DELETE /api/products/{id}/comments/{commentId} - Удалить комментарий
```

#### Модерация (`/api/moderation`)

```
GET  /api/moderation/products              - Товары на модерацию
GET  /api/moderation/products/{id}         - Товар на модерацию
POST /api/moderation/products/{id}/approve - Одобрить товар
POST /api/moderation/products/{id}/reject  - Отклонить товар
```

### Пример использования

#### 1. Регистрация

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePassword123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

Ответ:

```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

#### 2. Создание магазина

```bash
curl -X POST http://localhost:8080/api/shops \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {accessToken}" \
  -d '{
    "name": "My Shop",
    "description": "Best products",
    "avatarUrl": "https://example.com/avatar.jpg"
  }'
```

#### 3. Создание товара

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {accessToken}" \
  -d '{
    "name": "Laptop",
    "description": "High-performance laptop",
    "price": 1500.00,
    "imageUrl": "https://example.com/laptop.jpg",
    "shopId": 1
  }'
```

#### 4. Добавление товара в корзину

```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {accessToken}" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

## 🏗 Архитектура базы данных

### ER Диаграмма

```
User (1) -----> (1) Shop
  |                   |
  | (1:N)             | (1:N)
  |                   |
  v                   v
Order           Product
  |                   |
  | (N:M)             |
  |                   |
  +---OrderItem-------+
  
User (1) -----> (N) Comment <----- (N) Product
```

### Схема таблиц

#### `users`

| Колонка | Тип | Особенности |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `username` | VARCHAR(32) | UNIQUE NOT NULL |
| `email` | VARCHAR(255) | UNIQUE NOT NULL |
| `password` | VARCHAR(255) | NOT NULL (bcrypt) |
| `first_name` | VARCHAR(100) | NOT NULL |
| `last_name` | VARCHAR(100) | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

#### `user_roles`

| Колонка | Тип | Особенности |
|---|---|---|
| `user_id` | BIGINT | PRIMARY KEY, FK users(id) |
| `role` | VARCHAR(50) | PRIMARY KEY, enum: USER, SELLER, MODERATOR, ADMIN |

#### `shops`

| Колонка | Тип | Особенности |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `name` | VARCHAR(200) | NOT NULL |
| `description` | TEXT | |
| `avatar_url` | VARCHAR(500) | |
| `seller_id` | BIGINT | UNIQUE NOT NULL, FK users(id) |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

#### `products`

| Колонка | Тип | Особенности |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `name` | VARCHAR(255) | NOT NULL |
| `description` | TEXT | |
| `price` | NUMERIC(19,2) | NOT NULL, CHECK > 0 |
| `image_url` | VARCHAR(500) | |
| `shop_id` | BIGINT | NOT NULL, FK shops(id) |
| `seller_id` | BIGINT | NOT NULL, FK users(id) |
| `status` | VARCHAR(50) | NOT NULL, enum: PENDING, APPROVED, REJECTED |
| `rejection_reason` | TEXT | |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

#### `orders`

| Колонка | Тип | Особенности |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `user_id` | BIGINT | NOT NULL, FK users(id) |
| `total_price` | NUMERIC(19,2) | NOT NULL, DEFAULT 0, CHECK >= 0 |
| `status` | VARCHAR(50) | NOT NULL, enum: CART, PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELED |
| `delivery_address` | TEXT | |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

#### `order_items`

| Колонка | Тип | Особенности |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `order_id` | BIGINT | NOT NULL, FK orders(id) |
| `product_id` | BIGINT | NOT NULL, FK products(id) |
| `quantity` | INTEGER | NOT NULL, CHECK > 0 |
| `price` | NUMERIC(19,2) | NOT NULL, CHECK > 0 |
| `created_at` | TIMESTAMP | NOT NULL |
| UNIQUE | (order_id, product_id) | |

#### `comments`

| Колонка | Тип | Особенности |
|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY |
| `product_id` | BIGINT | NOT NULL, FK products(id) |
| `user_id` | BIGINT | NOT NULL, FK users(id) |
| `text` | TEXT | NOT NULL |
| `rating` | INTEGER | NOT NULL, CHECK between 1 and 5 |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

## 📁 Структура проекта

```
backend/
├── src/
│   ├── main/
│   │   ├── kotlin/ru/itmo/market/
│   │   │   ├── ItmoMarketApplication.kt
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.kt
│   │   │   │   └── OpenApiConfig.kt
│   │   │   ├── controller/
│   │   │   │   ├── AuthUserControllers.kt
│   │   │   │   ├── ProductShopCartOrderControllers.kt
│   │   │   │   └── CommentModerationControllers.kt
│   │   │   ├── service/
│   │   │   │   ├── AuthService.kt
│   │   │   │   ├── ProductService.kt
│   │   │   │   ├── OrderService.kt
│   │   │   │   ├── ShopService.kt
│   │   │   │   └── UserCommentModerationServices.kt
│   │   │   ├── repository/
│   │   │   │   └── Repositories.kt
│   │   │   ├── model/
│   │   │   │   ├── entity/
│   │   │   │   │   └── Entities.kt
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/
│   │   │   │   │   │   └── RequestDTOs.kt
│   │   │   │   │   └── response/
│   │   │   │   │       └── ResponseDTOs.kt
│   │   │   │   └── enums/
│   │   │   │       └── Enums.kt
│   │   │   ├── security/
│   │   │   │   ├── CustomUserDetails.kt
│   │   │   │   ├── UserDetailsServiceImpl.kt
│   │   │   │   └── jwt/
│   │   │   │       ├── JwtTokenProvider.kt
│   │   │   │       └── JwtAuthenticationFilter.kt
│   │   │   └── exception/
│   │   │       ├── CustomExceptions.kt
│   │   │       └── GlobalExceptionHandler.kt
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-test.yml
│   │       └── db/migration/
│   │           └── V1__Initial_schema.sql
│   └── test/
│       └── kotlin/ru/itmo/market/
│           ├── integration/
│           │   └── IntegrationTests.kt
│           └── service/
│               └── UnitTests.kt
├── build.gradle.kts
├── settings.gradle.kts
├── Dockerfile
└── .dockerignore
```

## 🧪 Тестирование

### Unit Tests

```bash
# Запуск всех unit тестов
./gradlew test

# Запуск конкретного теста
./gradlew test --tests AuthServiceUnitTest

# С отчетом о покрытии
./gradlew test jacocoTestReport
```

### Integration Tests

```bash
# Запуск интеграционных тестов (требует Testcontainers)
./gradlew integrationTest

# Или через test с флагом
./gradlew test -Dintegration=true
```

### Test Coverage

```bash
# Генерирование отчета о покрытии
./gradlew jacocoTestReport

# Просмотр отчета
open build/reports/jacoco/test/html/index.html
```

## 🐳 Развертывание

### Docker Compose

```bash
# Полный запуск всех компонентов
docker-compose up -d

# Просмотр статуса сервисов
docker-compose ps

# Просмотр логов
docker-compose logs -f

# Остановка и удаление контейнеров
docker-compose down

# Удаление контейнеров и данных
docker-compose down -v
```

### Проверка здоровья

```bash
# Проверить статус приложения
curl http://localhost:8080/actuator/health

# Просмотр информации о приложении
curl http://localhost:8080/actuator/info

# Метрики приложения
curl http://localhost:8080/actuator/metrics
```

### Adminer

Database management UI доступен на http://localhost:8081

- **System**: PostgreSQL
- **Server**: postgres
- **Username**: itmo_user
- **Password**: itmo_password
- **Database**: itmo_market

## 🔐 Безопасность

### JWT Authentication

Все защищенные endpoints требуют заголовок:

```
Authorization: Bearer {accessToken}
```

### Пароли

Пароли хранятся с использованием bcrypt. Минимальные требования:
- Длина: 8-72 символа
- Тип: String

## 📝 Логирование

Логи находятся в:

```
# Console
- INFO уровень по умолчанию
- DEBUG для ru.itmo.market пакета

# Файлы (если настроены)
logs/
├── app.log
└── error.log
```

## 🤝 Внесение улучшений

```bash
# Создание ветки для разработки
git checkout -b feature/your-feature

# Внесение изменений и коммит
git add .
git commit -m "Add your feature"

# Push в репозиторий
git push origin feature/your-feature

# Создание Pull Request
```

## 📄 Лицензия

Проект лицензирован под MIT License.

## 👥 Контакты и поддержка

- **Issues**: https://github.com/yourusername/itmo-market/issues
- **Discussions**: https://github.com/yourusername/itmo-market/discussions
- **Email**: support@itmo-market.local

## 📚 Дополнительные ресурсы

- [Spring Boot документация](https://spring.io/projects/spring-boot)
- [Kotlin документация](https://kotlinlang.org/docs/home.html)
- [PostgreSQL документация](https://www.postgresql.org/docs/)
- [JWT.io](https://jwt.io/)
- [OpenAPI 3.0 спецификация](https://spec.openapis.org/oas/v3.0.0)

---

**Последнее обновление**: 2025-11-06
**Версия**: 1.0.0
