# Development Setup

## Runtime

- Java 17+
- Spring Boot 3.5.x
- MySQL 8
- Redis 7
- Vue 3 + Vite 6
- Node.js 20+

## Docker ports

- MySQL: `13306 -> 3306`
- Redis: `16379 -> 6379`
- MinIO API: `19000 -> 9000`
- MinIO Console: `19001 -> 9001`
- Backend: `18080 -> 8080`
- Frontend: `8088 -> 80`

## Start all services

```bash
docker compose up -d --build
```

A fresh MySQL volume executes SQL files in lexical order. `sql/z_research_flow.sql` deliberately uses the `z_` prefix so it runs after the RuoYi base schema.

To force a clean database:

```bash
docker compose down -v
docker compose up -d --build
```

## Backend outside Docker

Start infrastructure only:

```bash
docker compose up -d mysql redis minio
```

Set:

```env
DB_HOST=localhost
DB_PORT=13306
DB_NAME=research_flow
DB_USERNAME=root
DB_PASSWORD=password
REDIS_HOST=localhost
REDIS_PORT=16379
MINIO_ENDPOINT=http://localhost:19000
MINIO_ACCESS_KEY=researchflow
MINIO_SECRET_KEY=researchflow123
MINIO_BUCKET=research-flow
```

Then:

```bash
mvn -DskipTests package
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

## Frontend outside Docker

```bash
cd ruoyi-ui
npm ci
npm run dev
```

`VITE_APP_BASE_URL` defaults to `http://localhost:18080`.

## AI Copilot

AI is optional. Add a root `.env` file when needed:

```env
DEEPSEEK_API_KEY=...
DEEPSEEK_ENDPOINT=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat
```

No AI key is required for any deterministic business workflow.
