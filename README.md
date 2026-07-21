# PaperTrade

A Wealthsimple-inspired **paper-trading platform** -- practice trading real US equities with $100,000 in play money, live prices, price alerts, and a portfolio that tracks its own growth over time.

Built as a fully reactive Java backend (Spring WebFlux) with a React + TypeScript frontend, and deployable to AWS via Terraform.

## Demo

▶ **[Watch demo 1](media/demo.mov)** · **[Watch demo 2](media/demo2.mov)**

<video src="media/demo.mov" controls width="100%">
  Your browser can't play this embedded video — <a href="media/demo.mov">download / view it here</a>.
</video>

<video src="media/demo2.mov" controls width="100%">
  Your browser can't play this embedded video — <a href="media/demo2.mov">download / view it here</a>.
</video>

## Features

- **Authentication** — register/login with JWT access + refresh tokens (BCrypt-hashed passwords). New users are provisioned a $100k account.
- **Trading** — market & limit orders, buy/sell, with pessimistic-locked balance updates and idempotent submission.
- **Market hours & order queue** — orders placed while the market is closed (or resting limit orders) are **queued** and filled by a background worker when conditions are met.
- **Live prices** — a Finnhub WebSocket feed is streamed to the browser over **Server-Sent Events**; quotes, position values, and the portfolio total tick in real time.
- **Watchlist & price alerts** — track symbols, set directional alerts that fire off the live price stream into a notifications feed.
- **Portfolio growth chart** — a scheduled job snapshots total portfolio value so the dashboard chart reflects *real* history.
- **Stock charts** — historical OHLC candles (1D / 1W / 3M / 1Y / YTD) with period change.
- **Activity** — full order and transaction history.

## Architecture

```
                    React + TypeScript (Vite, Tailwind, Chart.js)
                                   │  REST + SSE
                                   ▼
                     Spring WebFlux backend (Netty, fully reactive)
   ┌───────────────┬───────────────┼────────────────┬──────────────────┐
   ▼               ▼               ▼                ▼                  ▼
 R2DBC          Redis          Finnhub          Twelve Data          SQS
 Postgres    (quote cache)  (quotes, search,   (historical         (order queue;
 (Flyway)                    WebSocket stream)   candles)          local = DB poller)
```

Two market-data providers sit behind interfaces (Dependency Inversion): **Finnhub** for real-time quotes / search / streaming, **Twelve Data** for historical candles.

### Domain model

```
User (1) ──→ (1) TradingAccount
  │                   ├──→ (N) Position            (current holdings)
  │                   ├──→ (N) Order               (market/limit, lifecycle)
  │                   ├──→ (N) Transaction         (immutable fill log)
  │                   └──→ (N) PortfolioSnapshot   (value over time)
  ├──→ (N) WatchlistItem   (tracked symbols + alerts)
  └──→ (N) Notification    (triggered alerts)
```

## Tech stack

| Layer       | Technology                                                                                   |
| ----------- | -------------------------------------------------------------------------------------------- |
| Backend     | Java 21, Spring Boot 3.2**WebFlux** (reactive), Spring Security (reactive JWT)         |
| Data        | PostgreSQL via**R2DBC**, **Flyway** migrations, **Redis** (reactive) cache |
| Market data | Finnhub (REST + WebSocket), Twelve Data (REST)                                               |
| Async       | AWS SQS order queue (profile-gated), scheduled workers                                       |
| Frontend    | React 18, TypeScript, Vite, Tailwind CSS, Chart.js, Axios                                    |
| Infra       | Docker,**Terraform** (ECS Fargate, RDS, ElastiCache, SQS, ALB, ECR), GitHub Actions    |

## Design highlights

- **Fully reactive, end to end** — Netty + R2DBC + WebClient. Security context lives in the Reactor Context, so it survives the thread hops a reactive request makes (a servlet/thread-local setup can't).
- **Locks never span network I/O** — the market price is fetched *before* the order transaction opens, so the pessimistic `FOR UPDATE` lock on the account is held only for fast local writes.
- **Provider abstraction (Strategy / DIP)** — `MarketDataService`, `HistoryProvider`, and `OrderQueue` are interfaces with swappable implementations (e.g. local DB-poller vs. SQS for the order queue).
- **Push via SSE + Observer** — one shared Finnhub WebSocket fans out through a multicast sink to the SSE stream *and* the price-alert engine.
- **Capture vs. compute** — portfolio history can't be reconstructed from today's prices, so it's snapshotted on a schedule.
- **Versioned schema & externalized secrets** — Flyway migrations run at startup; secrets come from the environment (SSM in AWS), never the image.

## Project structure

```
PaperTrade/
├── backend/                     # Spring WebFlux API
│   ├── src/main/java/com/papertrade/
│   │   ├── domain/              # Entities + enums
│   │   ├── repository/          # R2DBC repositories (pessimistic locking)
│   │   ├── service/             # Order/Portfolio/MarketData/Stream/Watchlist/Snapshot…
│   │   ├── controller/          # REST + SSE endpoints
│   │   ├── security/            # Reactive JWT filter + JwtService
│   │   ├── config/              # Security, Redis, R2DBC, SQS, WebClient
│   │   └── dto/ · exception/
│   ├── src/main/resources/db/migration/   # Flyway migrations
│   └── Dockerfile
├── frontend/                    # React + TypeScript SPA
│   └── src/{pages,components,context,hooks,services,types}
├── terraform/                   # AWS IaC (backend only)
├── .github/workflows/           # CI/CD (build → ECR → ECS)
└── docker-compose.yml           # Local Postgres + Redis
```

## Getting started (local)

### Prerequisites

- Java 21 (`brew install openjdk@21`)
- Docker + Docker Compose
- Node 20+
- Free API keys: [Finnhub](https://finnhub.io/register) and [Twelve Data](https://twelvedata.com/pricing)

### 1. Start Postgres + Redis

```bash
docker-compose up -d
```

> Postgres is mapped to host port **5433** to avoid colliding with a local Postgres on 5432.

### 2. Configure secrets

```bash
cd backend
cp .env.example .env      # then set FINNHUB_API_KEY and TWELVE_DATA_API_KEY
```

### 3. Run the backend

```bash
./gradlew bootRun         # http://localhost:8080  (Flyway migrates the schema on startup)
```

### 4. Run the frontend

```bash
cd ../frontend
npm install
npm run dev               # http://localhost:3000
```

Open http://localhost:3000, **register an account**, and start trading.

## API reference

| Area          | Endpoint                                                                                   |
| ------------- | ------------------------------------------------------------------------------------------ |
| Auth          | `POST /api/auth/register` · `POST /api/auth/login` · `POST /api/auth/refresh`      |
| Portfolio     | `GET /api/portfolio` · `GET /api/portfolio/history`                                   |
| Orders        | `POST /api/orders` · `GET /api/orders` · `DELETE /api/orders/{id}`                 |
| Transactions  | `GET /api/transactions`                                                                  |
| Market data   | `GET /api/stocks/{symbol}/quote` · `/history?range=1D\|1W\|3M\|1Y\|YTD` · `/search?q=` |
| Live prices   | `GET /api/stream/prices?symbols=AAPL,GOOGL` (SSE)                                        |
| Watchlist     | `GET /api/watchlist` · `POST /api/watchlist` · `DELETE /api/watchlist/{symbol}`    |
| Notifications | `GET /api/notifications` · `/unread-count` · `POST /api/notifications/read`        |
| Health        | `GET /actuator/health`                                                                   |

Protected endpoints require `Authorization: Bearer <token>`; auth, market-data, and the SSE stream are public.

## Deployment

Backend infrastructure is defined as Terraform in [`terraform/`](terraform/) — VPC, ECS Fargate, ALB, RDS Postgres, ElastiCache Redis, SQS (+ DLQ), ECR, SSM-backed secrets, and least-privilege IAM. Only the ALB is public; the database, cache, and tasks live in private subnets. See [terraform/README.md](terraform/README.md).

```bash
cd terraform
terraform init
terraform plan            # review — no cost
# terraform apply         # provisions billable resources
```

CI/CD ([`.github/workflows/deploy.yml`](.github/workflows/deploy.yml)) builds the backend image, pushes to ECR, and rolls the ECS service.

## Notes

Design notes and OOD reasoning live in [NOTES.md](NOTES.md).
