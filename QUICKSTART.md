# PaperTrade - Quick Start Guide

Get your paper trading app running in 5 minutes!

## Prerequisites

- Java 21
- Docker & Docker Compose
- Finnhub API Key (free): https://finnhub.io/register

## Step 1: Start Dependencies

Start PostgreSQL and Redis:

```bash
docker-compose up -d
```

Verify they're running:
```bash
docker ps
# Should see: papertrade-postgres and papertrade-redis
```

## Step 2: Configure Environment

Copy the example environment file:
```bash
cd backend
cp .env.example .env
```

Edit `.env` and add your Finnhub API key:
```bash
FINNHUB_API_KEY=your_actual_api_key_here
```

## Step 3: Run the Backend

```bash
cd backend
./gradlew bootRun
```

Backend will start on `http://localhost:8080`

## Step 4: Test the API

### Get a stock quote:
```bash
curl http://localhost:8080/api/stocks/AAPL/quote
```

Expected response:
```json
{
  "symbol": "AAPL",
  "price": 175.43
}
```

## API Endpoints Overview

### Market Data
- `GET /api/stocks/{symbol}/quote` - Get current price
- `GET /api/stocks/search?q={query}` - Search stocks

### Trading
- `POST /api/orders` - Place order
- `GET /api/orders?accountId={uuid}` - Order history
- `DELETE /api/orders/{orderId}?accountId={uuid}` - Cancel order

### Portfolio
- `GET /api/portfolio?userId={uuid}` - Get portfolio summary

## Example: Place a Buy Order

First, you'll need to create an account (we'll add auth later). For now, let's manually create one:

```bash
# Connect to PostgreSQL
docker exec -it papertrade-postgres psql -U postgres -d papertrade

# Create a test user and account
INSERT INTO users (user_id, username, email, hashed_password, enabled)
VALUES ('123e4567-e89b-12d3-a456-426614174000', 'testuser', 'test@example.com', 'password', true);

INSERT INTO accounts (account_id, user_id, balance, initial_balance)
VALUES ('223e4567-e89b-12d3-a456-426614174000', '123e4567-e89b-12d3-a456-426614174000', 100000.00, 100000.00);

\q
```

Now place a buy order:
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "223e4567-e89b-12d3-a456-426614174000",
    "symbol": "AAPL",
    "type": "MARKET",
    "side": "BUY",
    "quantity": 10,
    "idempotencyKey": "test-order-1"
  }'
```

Expected response:
```json
{
  "orderId": "...",
  "symbol": "AAPL",
  "type": "MARKET",
  "side": "BUY",
  "quantity": 10,
  "status": "FILLED",
  "filledPrice": 175.43,
  "filledQuantity": 10,
  "createdAt": "2026-07-18T10:30:00",
  "filledAt": "2026-07-18T10:30:00"
}
```

Check your portfolio:
```bash
curl "http://localhost:8080/api/portfolio?userId=123e4567-e89b-12d3-a456-426614174000"
```

## Troubleshooting

### Database connection error?
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs papertrade-postgres
```

### Redis connection error?
```bash
# Check if Redis is running
docker ps | grep redis

# Test Redis
docker exec -it papertrade-redis redis-cli ping
# Should return: PONG
```

### Finnhub API error?
- Verify your API key is correct in `.env`
- Check rate limits (free tier: 60 calls/minute)

## Frontend Setup

### Step 5: Install Frontend Dependencies

```bash
cd ../frontend
npm install
```

### Step 6: Run the Frontend

```bash
npm run dev
```

Frontend will start on `http://localhost:3000`

### Step 7: Access the App

Open your browser to `http://localhost:3000`

You should see the PaperTrade dashboard!

**Note:** The app uses temporary hardcoded user/account IDs. You'll need to create these in the database (see manual account creation above).

## Frontend Features

✅ **Dashboard** - Portfolio overview with Chart.js visualization  
✅ **Positions** - View all your stock holdings with P&L  
✅ **Stock Detail** - Buy/sell stocks (Market & Limit orders)  
✅ **Search** - Find stocks to trade  
✅ **Minimalist UI** - Clean, Wealthsimple-inspired design

## Next Steps

1. **Add Authentication** - JWT-based auth for secure access
2. **Real-time Updates** - WebSocket for live price feeds
3. **Deploy to AWS** - ECS Fargate + RDS + ElastiCache

---

## Interview Prep - Key Talking Points

When discussing this with Wealthsimple:

### OOD Highlights:
- **Pessimistic Locking**: See `OrderService.executeBuyOrder()` line 59
- **Design Patterns**: Strategy, Observer, Repository, Factory
- **SOLID Principles**: Dependency Inversion (MarketDataService interface)

### Concurrency:
```java
// OrderService.java line 59
accountRepository.findByIdWithLock(accountId) // PESSIMISTIC LOCK
    .flatMap(account -> {
        // Critical section - balance update safe from races
        account.debit(totalCost);
    });
```

### Caching Strategy:
```java
// FinnhubMarketDataService.java
// Cache-aside pattern with 5s TTL
redisTemplate.opsForValue().get(cacheKey)
    .switchIfEmpty(fetchFromApi().flatMap(cachePriceAndReturn));
```

Good luck with your Wealthsimple interview! 🚀
