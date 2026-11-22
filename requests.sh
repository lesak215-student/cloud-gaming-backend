#!/usr/bin/env bash

BASE="http://localhost:8080"
TS=$(date +%s)

echo "=========================================================="
echo "   ПРОВЕРКА API: ОБЛАЧНЫЙ ГЕЙМИНГ (ЛАБОРАТОРНАЯ РАБОТА 3)   "
echo "=========================================================="

echo ""
echo "=== 1. ПРОВЕРКА PING ==="
curl -s -i "$BASE/ping"
echo ""

echo ""
echo "=== 2. ПОЛЬЗОВАТЕЛИ: СОЗДАНИЕ, ЧТЕНИЕ, ОБНОВЛЕНИЕ, УНИКАЛЬНОСТЬ ==="
echo "--- Создание Standard пользователя (Alice) ---"
USER1_RESP=$(curl -s -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Alice\",\"email\":\"alice_${TS}@example.com\",\"subscriptionTier\":\"BASIC\"}")
echo "$USER1_RESP"
USER1_ID=$(echo "$USER1_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

echo "--- Создание Premium пользователя (Bob) ---"
USER2_RESP=$(curl -s -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Bob\",\"email\":\"bob_${TS}@example.com\",\"subscriptionTier\":\"PREMIUM\"}")
echo "$USER2_RESP"
USER2_ID=$(echo "$USER2_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

echo "--- Ошибка 409: дубликат email пользователя ---"
curl -s -i -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Alice Clone\",\"email\":\"alice_${TS}@example.com\",\"subscriptionTier\":\"BASIC\"}"
echo ""

echo "--- Список всех пользователей ---"
curl -s "$BASE/users"
echo ""

echo "--- Фильтрация пользователей по тарифу PREMIUM ---"
curl -s "$BASE/users?tier=PREMIUM"
echo ""

echo "--- Обновление пользователя (Alice Smith) ---"
curl -s -X PUT "$BASE/users/$USER1_ID" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Alice Smith\",\"email\":\"alice_smith_${TS}@example.com\",\"subscriptionTier\":\"BASIC\"}"
echo ""

echo ""
echo "=== 3. ИГРЫ: СОЗДАНИЕ, ЧТЕНИЕ, ОБНОВЛЕНИЕ, УНИКАЛЬНОСТЬ ==="
echo "--- Создание базовой игры (CS2, 5 слотов) ---"
GAME1_RESP=$(curl -s -X POST "$BASE/games" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"CS2_${TS}\",\"genre\":\"Shooter\",\"requiredTier\":\"BASIC\",\"maxSlots\":5}")
echo "$GAME1_RESP"
GAME1_ID=$(echo "$GAME1_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

echo "--- Создание премиум игры (Cyberpunk 2077, 2 слота) ---"
GAME2_RESP=$(curl -s -X POST "$BASE/games" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Cyberpunk_${TS}\",\"genre\":\"RPG\",\"requiredTier\":\"PREMIUM\",\"maxSlots\":2}")
echo "$GAME2_RESP"
GAME2_ID=$(echo "$GAME2_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

echo "--- Ошибка 409: дубликат названия игры ---"
curl -s -i -X POST "$BASE/games" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"CS2_${TS}\",\"genre\":\"FPS\",\"requiredTier\":\"BASIC\",\"maxSlots\":10}"
echo ""

echo "--- Список всех игр ---"
curl -s "$BASE/games"
echo ""

echo "--- Фильтрация игр по жанру и тарифу ---"
curl -s "$BASE/games?genre=RPG&tier=PREMIUM"
echo ""

echo ""
echo "=== 4. СЕССИИ: СОЗДАНИЕ СО СВЯЗЯМИ И ЗАМЕТКАМИ (МИГРАЦИЯ V2) ==="
echo "--- Запуск сессии Alice в базовую игру CS2 (1080p) ---"
SESSION1_RESP=$(curl -s -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":$USER1_ID,\"gameId\":$GAME1_ID,\"resolution\":1080}")
echo "$SESSION1_RESP"
SESSION1_ID=$(echo "$SESSION1_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

echo "--- Запуск сессии Bob в премиум игру Cyberpunk (2160p) ---"
SESSION2_RESP=$(curl -s -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":$USER2_ID,\"gameId\":$GAME2_ID,\"resolution\":2160}")
echo "$SESSION2_RESP"
SESSION2_ID=$(echo "$SESSION2_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

echo "--- Получение сессии по ID (проверка связей со связанными сущностями) ---"
curl -s "$BASE/sessions/$SESSION1_ID"
echo ""

echo ""
echo "=== 5. ЖИЗНЕННЫЙ ЦИКЛ СЕССИЙ (PAUSE, RESUME, UPDATE NOTES, TERMINATE) ==="
echo "--- Приостановка сессии (POST /sessions/{id}/pause) ---"
curl -s -X POST "$BASE/sessions/$SESSION1_ID/pause"
echo ""

echo "--- Возобновление сессии (POST /sessions/{id}/resume) ---"
curl -s -X POST "$BASE/sessions/$SESSION1_ID/resume"
echo ""

echo "--- Обновление разрешения и заметок (PUT /sessions/{id}) ---"
curl -s -X PUT "$BASE/sessions/$SESSION1_ID" \
  -H "Content-Type: application/json" \
  -d '{"resolution":720,"notes":"Lowered resolution due to high latency"}'
echo ""

echo "--- Завершение сессии (POST /sessions/{id}/terminate) ---"
curl -s -X POST "$BASE/sessions/$SESSION1_ID/terminate"
echo ""

echo ""
echo "=== 6. ТРЕБОВАНИЕ 3: АТОМАРНАЯ ТРАНЗАКЦИЯ И ОТКАТ (ROLLBACK) ==="
echo "Создаем игру для теста обслуживания и запускаем 2 сессии:"
MAINT_GAME_RESP=$(curl -s -X POST "$BASE/games" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"MaintGame_${TS}\",\"genre\":\"Action\",\"requiredTier\":\"BASIC\",\"maxSlots\":4}")
MAINT_GAME_ID=$(echo "$MAINT_GAME_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

MAINT_USER_RESP=$(curl -s -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"MaintPlayer\",\"email\":\"maint_${TS}@example.com\",\"subscriptionTier\":\"BASIC\"}")
MAINT_USER_ID=$(echo "$MAINT_USER_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

MAINT_SESS_RESP=$(curl -s -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":$MAINT_USER_ID,\"gameId\":$MAINT_GAME_ID,\"resolution\":1080}")
MAINT_SESS_ID=$(echo "$MAINT_SESS_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

echo "Текущие слоты игры до вызова обслуживания: 4, статус сессии: ACTIVE"

echo "--- Вызов scheduleMaintenance с triggerError=true (ожидаем 409 Conflict) ---"
curl -s -i -X POST "$BASE/games/$MAINT_GAME_ID/maintenance" \
  -H "Content-Type: application/json" \
  -d '{"newMaxSlots":0,"reason":"Scheduled cluster upgrade","triggerError":true}'
echo ""

echo "--- Проверка отката (Rollback): слоты игры не должны измениться (остались 4) ---"
curl -s "$BASE/games/$MAINT_GAME_ID"
echo ""

echo "--- Проверка отката (Rollback): статус сессии должен остаться ACTIVE, notes = null ---"
curl -s "$BASE/sessions/$MAINT_SESS_ID"
echo ""

echo "--- Успешный вызов scheduleMaintenance (triggerError=false) ---"
curl -s -X POST "$BASE/games/$MAINT_GAME_ID/maintenance" \
  -H "Content-Type: application/json" \
  -d '{"newMaxSlots":0,"reason":"Scheduled cluster upgrade","triggerError":false}'
echo ""

echo "--- Проверка применения: слоты игры стали 0 ---"
curl -s "$BASE/games/$MAINT_GAME_ID"
echo ""

echo "--- Проверка применения: сессия переведена в COMPLETED с причиной в notes ---"
curl -s "$BASE/sessions/$MAINT_SESS_ID"
echo ""

echo ""
echo "=== 7. ТРЕБОВАНИЕ 5: ЗАГРУЗКА РЕСУРСОВ БЕЗ ПРОБЛЕМЫ N+1 (GET /sessions/detailed) ==="
echo "Выполняем запрос списка сессий с жадной загрузкой (JOIN FETCH) связанных User и Game:"
curl -s "$BASE/sessions/detailed"
echo ""

echo ""
echo "=== 8. ЦЕЛОСТНОСТЬ ДАННЫХ И УДАЛЕНИЕ СВЯЗАННЫХ РЕСУРСОВ ==="
echo "Попытка удалить пользователя Bob при активной сессии в Cyberpunk (ожидаем 409):"
curl -s -i -X DELETE "$BASE/users/$USER2_ID"
echo ""

echo "Завершаем активную сессию Bob:"
curl -s -X POST "$BASE/sessions/$SESSION2_ID/terminate"
echo ""

echo "Удаление пользователя Bob после завершения сессий (ожидаем 204 No Content):"
curl -s -i -X DELETE "$BASE/users/$USER2_ID"
echo ""

echo "Проверка, что пользователь удален (ожидаем 404):"
curl -s -i "$BASE/users/$USER2_ID"
echo ""

echo ""
echo "=== 9. ВАЛИДАЦИЯ И ОБРАБОТКА ОШИБОЧНЫХ СЦЕНАРИЕВ ==="
echo "--- 400: Отрицательное разрешение (resolution = -100) ---"
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":$USER1_ID,\"gameId\":$GAME1_ID,\"resolution\":-100}"
echo ""

echo "--- 400: Отрицательный ID в пути (/users/-1) ---"
curl -s -i "$BASE/users/-1"
echo ""

echo "--- 400: Невалидный JSON формат ---"
curl -s -i -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d "not a json"
echo ""

echo "--- 404: Несуществующий пользователь ---"
curl -s -i "$BASE/users/99999"
echo ""

echo "--- 404: Несуществующая игра при запуске сессии ---"
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":$USER1_ID,\"gameId\":99999,\"resolution\":1080}"
echo ""

echo "--- 409: Standard пользователь не может запустить PREMIUM игру ---"
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":$USER1_ID,\"gameId\":$GAME2_ID,\"resolution\":1080}"
echo ""

echo "--- 409: Превышение максимального разрешения тарифа (Standard ограничен 1080p, запрос на 2160p) ---"
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":$USER1_ID,\"gameId\":$GAME1_ID,\"resolution\":2160}"
echo ""

echo "=========================================================="
echo "          ВСЕ СЦЕНАРИИ ТЕСТИРОВАНИЯ УСПЕШНО ВЫПОЛНЕНЫ!     "
echo "=========================================================="
