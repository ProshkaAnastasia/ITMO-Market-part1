#!/bin/bash

# ITMO-Market Backend - Скрипт для быстрого запуска
# Usage: bash setup.sh

set -e

echo "🚀 ITMO-Market Backend - Инициализация проекта"
echo "================================================"

# Проверка Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker не установлен. Пожалуйста, установите Docker."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose не установлен. Пожалуйста, установите Docker Compose."
    exit 1
fi

echo "✅ Docker установлен"
echo "✅ Docker Compose установлен"

# Создание .env файла
if [ ! -f .env ]; then
    echo "📝 Создание .env файла..."
    cp .env.example .env
    echo "✅ .env файл создан"
else
    echo "✅ .env файл уже существует"
fi

# Остановка старых контейнеров
echo "🛑 Остановка предыдущих контейнеров..."
docker-compose down 2>/dev/null || true

# Запуск Docker Compose
echo "🐳 Запуск Docker Compose..."
docker-compose up -d

echo ""
echo "⏳ Ожидание инициализации сервисов..."
sleep 10

# Проверка здоровья
echo ""
echo "🏥 Проверка здоровья приложения..."
max_retries=30
retry_count=0

while [ $retry_count -lt $max_retries ]; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ Backend готов"
        break
    else
        echo "⏳ Ожидание инициализации backend... ($((retry_count+1))/$max_retries)"
        sleep 2
        retry_count=$((retry_count+1))
    fi
done

if [ $retry_count -eq $max_retries ]; then
    echo "❌ Backend не ответил за отведенное время"
    docker-compose logs backend
    exit 1
fi

# Успешный старт
echo ""
echo "================================================"
echo "✨ УСПЕШНО! Приложение готово к работе ✨"
echo "================================================"
echo ""
echo "📍 Основные адреса:"
echo "  API:                http://localhost:8080"
echo "  Swagger UI:         http://localhost:8080/swagger-ui.html"
echo "  OpenAPI Docs:       http://localhost:8080/v3/api-docs"
echo "  Database UI:        http://localhost:8081"
echo ""
echo "🔐 Учетные данные для Database UI:"
echo "  System:     PostgreSQL"
echo "  Server:     postgres"
echo "  Username:   itmo_user"
echo "  Password:   itmo_password"
echo "  Database:   itmo_market"
echo ""
echo "📚 Полезные команды:"
echo "  Просмотр логов:       docker-compose logs -f backend"
echo "  Остановка:            docker-compose down"
echo "  Перезапуск:           docker-compose restart backend"
echo "  Очистка данных:       docker-compose down -v"
echo ""
echo "💡 Первый тест API:"
echo "  curl -X POST http://localhost:8080/api/auth/register \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"username\":\"test\",\"email\":\"test@example.com\",\"password\":\"Test123\",\"firstName\":\"Test\",\"lastName\":\"User\"}'"
echo ""
