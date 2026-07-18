# PaperTrade

A Wealthsimple-inspired paper trading application built with Java Spring WebFlux, React, and PostgreSQL.

## Architecture Overview

### Backend (Java Spring WebFlux)

- **Reactive Programming**: Non-blocking I/O for high scalability
- **PostgreSQL + R2DBC**: Reactive database access
- **Redis**: Market data caching (5s TTL)
- **Finnhub WebSocket**: Real-time stock price updates
- **JWT Authentication**: Secure user sessions

### Key Design Patterns

1. **Strategy Pattern**: `OrderExecutionStrategy` (Market vs Limit orders)
2. **Observer Pattern**: Market data updates notify multiple listeners
3. **Repository Pattern**: Data access abstraction
4. **Factory Pattern**: Order creation

### Domain Model

```
User (1) ──→ (1) TradingAccount
                    │
                    ├──→ (N) Position (holdings)
                    ├──→ (N) Order (buy/sell orders)
                    └──→ (N) Transaction (audit log)
```

## Project Structure

```
backend/
├── src/main/java/com/papertrade/
│   ├── domain/          # Entity classes (User, Order, Position, etc.)
│   ├── repository/      # R2DBC repositories with pessimistic locking
│   ├── service/         # Business logic (OrderService, PortfolioService)
│   ├── controller/      # REST API endpoints
│   ├── config/          # Spring configuration
│   ├── security/        # JWT authentication
│   ├── dto/             # Data transfer objects
│   └── exception/       # Custom exceptions
├── src/main/resources/
│   ├── application.yml  # Configuration
│   └── schema.sql       # Database schema
└── build.gradle         # Dependencies
```

## API Endpoints (Planned)

### Authentication

- `POST /api/auth/register` - Create account
- `POST /api/auth/login` - Login & get JWT
- `POST /api/auth/refresh` - Refresh token

### Trading

- `POST /api/orders` - Place order (market/limit)
- `GET /api/orders` - Get order history
- `DELETE /api/orders/{id}` - Cancel pending order

### Portfolio

- `GET /api/portfolio` - Get portfolio summary (balance + positions)
- `GET /api/positions` - Get all positions
- `GET /api/transactions` - Get transaction history

### Market Data

- `GET /api/stocks/{symbol}/quote` - Get current price
- `GET /api/stocks/search?q={query}` - Search stocks

## Key Technical Decisions

### Concurrency Control

- **Pessimistic Locking**: Used for balance updates during order placement
  - Prevents race conditions when multiple orders submitted simultaneously
  - Repository method: `@Lock(LockModeType.PESSIMISTIC_WRITE)`

### Caching Strategy

- **Market Data**: Redis cache with 5s TTL (reduce Finnhub API calls)
- **Cache-Aside Pattern**: Fetch from cache → miss → fetch from API → store in cache

### Order Execution

- **Market Orders**: Execute immediately at current price
- **Limit Orders**: Execute only when price condition met
- **Idempotency**: Prevent duplicate orders via unique `idempotencyKey`

## Testing Strategy

- **Unit Tests**: Domain logic (Position P&L calculations, Order validation)
- **Integration Tests**: Repository layer with TestContainers
- **End-to-End Tests**: Full order placement flow

## Future Extensions

- **Options Trading**: Add `Option` subclass of abstract `Security` class
- **Real-Time Updates**: WebSocket streaming of portfolio changes
- **Advanced Orders**: Stop-loss, trailing stop, OCO orders
